package de.tuberlin.cit.freamon.monitor.actors

import java.io.FileInputStream
import java.net.InetAddress

import akka.actor.{Actor, ActorSelection, Address}
import akka.event.Logging
import com.typesafe.config.Config
import de.tuberlin.cit.freamon.collector.AppStatsCollector
import org.apache.hadoop.yarn.conf.YarnConfiguration

import scala.collection.mutable

case class StartRecording(applicationId: String, containerIds: Array[Long])

case class StopRecording(applicationId: String)

class MonitorAgentActor() extends Actor {

  val log = Logging(context.system, this)
  val applications = new mutable.HashMap[String, AppStatsCollector]
  var yarnConfig: YarnConfiguration = null

  override def preStart(): Unit = {
    log.info("Monitor Agent started")
    val hostConfig = context.system.settings.config

    this.getMasterActor(hostConfig) ! WorkerAnnouncement(InetAddress.getLocalHost.getHostName)

    val yarnSitePath = hostConfig.getString("freamon.hosts.slaves.yarnsite")
    log.info("Using yarn-site.xml at " + yarnSitePath)
    yarnConfig = new YarnConfiguration()
    yarnConfig.addResource(new FileInputStream(yarnSitePath), YarnConfiguration.YARN_SITE_CONFIGURATION_FILE)
  }

  def getMasterActor(hostConfig: Config): ActorSelection = {
    val masterSystemPath = new Address("akka.tcp",
      hostConfig.getString("freamon.actors.systems.master.name"),
      hostConfig.getString("freamon.hosts.master.hostname"),
      hostConfig.getInt("freamon.hosts.master.port"))

    val masterActorPath = masterSystemPath.toString + "/user/" + hostConfig.getString("freamon.actors.systems.master.actor")

    context.actorSelection(masterActorPath)
  }

  def receive = {

    case StartRecording(applicationId: String, containerIds: Array[Long]) =>
      log.info("Monitor Agent starts recording for app " + applicationId)
      log.info("Requested " + containerIds.length
        + " containers: " + containerIds.mkString(", "))

      applications.get(applicationId) match {
        case None => // new application
          val appStats = new AppStatsCollector(applicationId, yarnConfig, 1)
          appStats.addContainers(containerIds)
          // only record if this worker has at least one of the recorded containers
          if (appStats.containerStats.length <= 0) {
            log.info("This node has none of these containers, not recording anything")
          } else {
            applications(applicationId) = appStats
            appStats.onCollect = container => {
              log.info(container.containerId + " CPU avg: " + container.cpuUtil.last.formatted("%.2f  cores"))
              log.info(container.containerId + " Memory: " + container.memUtil.last + " MB")
            }
            appStats.startRecording()
            log.info("This node now records " + appStats.containerStats.length
              + " containers: " + appStats.containerCgroups.keys.mkString(", "))
          }

        case Some(appStats) => // already recording, just add the new containers
          appStats.addContainers(containerIds)
          log.info("This node now records " + appStats.containerStats.length
            + " containers: " + appStats.containerCgroups.keys.mkString(", "))
      }


    case StopRecording(applicationId: String) =>
      applications.get(applicationId) match {
        case None => log.info("Monitor Agent has no reports for " + applicationId)

        case Some(appStats) =>
          val containers = appStats.stopRecording()
          log.info("Monitor Agent sends " + containers.length + " reports for " + applicationId)
          for (container <- containers) {
            log.info(container.containerId + ": " + container.cpuUtil.length + " samples")
            sender ! new ContainerReport(applicationId, container)
          }
      }

  }

}
