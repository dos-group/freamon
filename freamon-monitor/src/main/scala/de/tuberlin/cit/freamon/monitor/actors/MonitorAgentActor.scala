package de.tuberlin.cit.freamon.monitor.actors

import java.io.FileInputStream
import java.net.InetAddress

import akka.actor.{Actor, ActorSelection, Address}
import akka.event.Logging
import com.typesafe.config.Config
import de.tuberlin.cit.freamon.collector.AppStatsCollector
import org.apache.hadoop.yarn.conf.YarnConfiguration

import scala.collection.mutable

case class StartRecording(applicationId: String, containerIds: Array[String])

case class StopRecording(applicationId: String)

class MonitorAgentActor() extends Actor {

  val log = Logging(context.system, this)
  val applications = new mutable.HashMap[String, AppStatsCollector]

  override def preStart(): Unit = {
    log.info("Monitor Agent started")
    val hostConfig = context.system.settings.config

    this.getMasterActor(hostConfig) ! WorkerAnnouncement(InetAddress.getLocalHost.getHostName)
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

    case StartRecording(applicationId: String, containerIds: Array[String]) =>
      log.info("Monitor Agent starts recording for app " + applicationId)
      log.info("Requested " + containerIds.length
        + " containers: " + containerIds.mkString(", "))

      val appStats = applications.getOrElse(applicationId, {
        val appStats = new AppStatsCollector(applicationId, 1)
        appStats.onCollect = container => {
          //log.debug(container.containerId + " BlkIO:  " + container.blkioUtil.last.formatted("%.2f sectors"))
          //log.debug(container.containerId + " Net:    " + container.netUtil.last.formatted("%.2f bytes"))
          log.debug(container.containerId + " CPU:    " + container.cpuUtil.last.formatted("%.2f cores"))
          log.debug(container.containerId + " Memory: " + container.memUtil.last + " MB")
        }
        applications(applicationId) = appStats
        appStats.startRecording()
      })

      appStats.addContainers(containerIds)

    case StopRecording(applicationId: String) =>
      applications.get(applicationId) match {
        case None => log.warning("Monitor Agent has no reports for " + applicationId)

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
