package de.tuberlin.cit.freamon.monitor.actors

import java.net.InetAddress

import akka.actor.{Actor, ActorSelection, Address}
import akka.event.Logging
import de.tuberlin.cit.freamon.collector.AppStatsCollector

import scala.collection.mutable

case class StartRecording(applicationId: String, containerIds: Array[Long])

case class StopRecording(applicationId: String)

class MonitorAgentActor(yarnSitePath: String) extends Actor {

  val log = Logging(context.system, this)
  val applications = new mutable.HashMap[String, AppStatsCollector]

  override def preStart(): Unit = {
    log.info("Monitor Agent started")
    this.getMasterActor ! WorkerAnnouncement(InetAddress.getLocalHost.getHostName)
  }

  def getMasterActor: ActorSelection = {
    val hostConfig = context.system.settings.config

    val masterSystemPath = new Address("akka.tcp", hostConfig.getString("freamon.actors.systems.master.name"),
      hostConfig.getString("freamon.hosts.master.hostname"), hostConfig.getInt("freamon.hosts.master.port"))

    val masterActorPath = masterSystemPath.toString + "/user/" + hostConfig.getString("freamon.actors.systems.master.actor")

    context.actorSelection(masterActorPath)
  }

  def receive = {

    case StartRecording(applicationId: String, containerIds: Array[Long]) =>
      log.info("Monitor Agent starts recording for app " + applicationId)
      log.info(containerIds.length + " containers: " + containerIds.mkString(", "))

      val appStats = new AppStatsCollector(applicationId, containerIds, yarnSitePath, 1)
      applications(applicationId) = appStats
      appStats.onCollect = () => {
        log.info("%s CPU avg: %.2f cores", applicationId, appStats.cpuUtil.last)
        log.info("%s Memory: %d MB", applicationId, appStats.memUtil.last)
      }
      appStats.startRecording()


    case StopRecording(applicationId: String) =>
      log.info("Monitor Agent sends Report for " + applicationId)
      val (cpuUtil, memUtil) = applications(applicationId).stopRecording()
      log.info(cpuUtil.length + " samples")
      sender ! new ContainerReport(applicationId, cpuUtil, memUtil)

  }

}
