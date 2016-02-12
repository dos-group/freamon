package de.tuberlin.cit.freamon.monitor.actors

import java.net.InetAddress

import akka.actor.{ActorSelection, Address, Actor}
import akka.event.Logging

case class StartRecording(applicationId: String, containerIds: Array[Long])

case class StopRecording(applicationId: String)

class MonitorAgentActor() extends Actor {

  val log = Logging(context.system, this)

  override def preStart(): Unit ={
    log.info("Monitor Master started")
    this.getMasterActor ! WorkerAnnouncement(InetAddress.getLocalHost.getHostName)
  }

  def getMasterActor:ActorSelection = {
    val hostConfig = context.system.settings.config

    val masterSystemPath = new Address("akka.tcp", hostConfig.getString("freamon.actors.systems.master.name"),
      hostConfig.getString("freamon.hosts.master.hostname"), hostConfig.getInt("freamon.hosts.master.port"))

    val masterActorPath = masterSystemPath.toString + "/user/" + hostConfig.getString("freamon.actors.systems.master.actor")

    context.actorSelection(masterActorPath)
  }

  def receive = {

    case StartRecording(applicationId: String, containerIds: Array[Long]) => {
      log.info("Monitor Agent starts recording for " + applicationId)
    }

    case StopRecording(applicationId: String) => {
      log.info("Monitor Agent sends Report for " + applicationId)
      sender ! new ContainerReport(applicationId, null, null)
    }

  }

}
