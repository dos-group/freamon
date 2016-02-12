package de.tuberlin.cit.freamon.monitor.actors

import akka.actor.{ActorSelection, Address, Actor}
import akka.event.Logging
import scala.collection.JavaConversions._

case class StartMonitoringForApplication(applicationId: String, containerIds: Array[Long])

case class StopMonitoringForApplication(applicationId: String)

case class ContainerReport(containerId: String, cpuUtil: Array[Float], memUtil: Array[Int])


class MonitorMasterActor extends Actor {
  val log = Logging(context.system, this)

  override def preStart(): Unit ={
    log.info("Monitor Master started")
  }

  def getAgentActorOnHost(hostname: String):ActorSelection = {
    val hostConfig = context.system.settings.config

    val agentSystem = new Address("akka.tcp", hostConfig.getString("freamon.actors.systems.slave.name"),
      hostname, hostConfig.getInt("freamon.hosts.slaves.port"))
    val agentActorPath = agentSystem.toString + "/user/" + hostConfig.getString("freamon.actors.systems.slave.actor")
    context.system.actorSelection(agentActorPath)
  }

  def receive = {

    case StartMonitoringForApplication(applicationId: String, containerIds: Array[Long]) => {
      val hostConfig = context.system.settings.config

      for (host <- hostConfig.getStringList("freamon.hosts.slaves.hostnames")) {
        val agentActor = this.getAgentActorOnHost(host)
        agentActor ! StartRecording(applicationId, containerIds)
      }
    }

    case ContainerReport(containerId, cpuUtil, memUtil) => {
      log.info("recv a container Report of " + containerId)
    }
  }
}
