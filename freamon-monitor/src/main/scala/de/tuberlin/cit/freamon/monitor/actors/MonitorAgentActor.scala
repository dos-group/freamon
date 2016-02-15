package de.tuberlin.cit.freamon.monitor.actors

import java.net.InetAddress

import akka.actor.{Actor, ActorSelection, Address}
import akka.event.Logging
import com.typesafe.config.Config
import de.tuberlin.cit.freamon.collector.{AppStatsCollector, YarnConfig}

import scala.collection.mutable

case class StartRecording(applicationId: String, containerIds: Array[Long])

case class StopRecording(applicationId: String)

class MonitorAgentActor() extends Actor {

  val log = Logging(context.system, this)
  val applications = new mutable.HashMap[String, AppStatsCollector]
  var yarnConfig: YarnConfig = null

  override def preStart(): Unit = {
    log.info("Monitor Agent started")
    val hostConfig = context.system.settings.config

    this.getMasterActor(hostConfig) ! WorkerAnnouncement(InetAddress.getLocalHost.getHostName)

    val yarnSitePath = hostConfig.getString("freamon.hosts.slaves.yarnsite")
    log.info("Using yarn-site.xml at " + yarnSitePath)
    yarnConfig = new YarnConfig(yarnSitePath)
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
      log.info(containerIds.length + " containers: " + containerIds.mkString(", "))

      val appStats = applications.getOrElseUpdate(applicationId,
        new AppStatsCollector(applicationId, containerIds, yarnConfig, 1))

      applications(applicationId) = appStats
      appStats.onCollect = () => {
        log.info(applicationId + " CPU avg: " + appStats.cpuUtil.last.formatted("%.2f  cores"))
        log.info(applicationId + " Memory: " + appStats.memUtil.last + " MB")
      }
      appStats.startRecording()


    case StopRecording(applicationId: String) =>
      log.info("Monitor Agent sends Report for " + applicationId)
      val (cpuUtil, memUtil) = applications(applicationId).stopRecording()
      log.info(cpuUtil.length + " samples")
      sender ! new ContainerReport(applicationId, cpuUtil, memUtil)

  }

}
