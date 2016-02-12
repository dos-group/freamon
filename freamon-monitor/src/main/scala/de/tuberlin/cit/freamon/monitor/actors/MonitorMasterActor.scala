package de.tuberlin.cit.freamon.monitor.actors

import akka.actor.{ActorSelection, Address, Actor}
import akka.event.Logging

import scala.collection.mutable

case class StartMonitoringForApplication(applicationId: String, containerIds: Array[Long])

case class StopMonitoringForApplication(applicationId: String)

case class WorkerAnnouncement(workerHostname: String)

case class ContainerReport(containerId: String, cpuUtil: Array[Float], memUtil: Array[Int])


class MonitorMasterActor extends Actor {

  val log = Logging(context.system, this)
  var workers: scala.collection.mutable.ListBuffer[String] = mutable.ListBuffer()

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
      for (host <- workers) {
        val agentActor = this.getAgentActorOnHost(host)
        agentActor ! StartRecording(applicationId, containerIds)
      }
    }

    case StopMonitoringForApplication(applicationId: String) => {
      for (host <- workers) {
        val agentActor = this.getAgentActorOnHost(host)
        agentActor ! StopRecording(applicationId)
      }
    }

    case WorkerAnnouncement(workerHostname) => {
      log.info(sender + " registered a new worker on " + workerHostname)
      workers += workerHostname
    }

    case ContainerReport(containerId, cpuUtil, memUtil) => {
      log.info("Received a container Report of " + containerId + " from " + sender)
    }

  }
}
