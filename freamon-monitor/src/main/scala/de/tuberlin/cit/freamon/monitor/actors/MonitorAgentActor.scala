package de.tuberlin.cit.freamon.monitor.actors

import akka.actor.{ActorSelection, Address, Actor}
import akka.event.Logging

case class StartRecording(applicationId: String, containerIds: Array[String])

case class StopRecording(applicationId: String)

class MonitorAgentActor() extends Actor {

  val log = Logging(context.system, this)

  def getMasterActor:ActorSelection = {
    val hostConfig = context.system.settings.config

    val masterSystemPath = new Address("akka.tcp", hostConfig.getString("freamon.actors.systems.master.name"),
      hostConfig.getString("freamon.hosts.master.hostname"), hostConfig.getInt("freamon.hosts.master.port"))

    val masterActorPath = masterSystemPath.toString + "/user/" + hostConfig.getString("freamon.actors.systems.master.actor")

    context.actorSelection(masterActorPath)
  }

  def receive = {

    case StartRecording(applicationId: String, containerIds: Array[String]) => {
      log.info("Monitor Agent starts recording for " + applicationId)
    }

    case StopRecording(applicationId: String) => {

      // TODO: use sender instead?
      val monitorMaster = this.getMasterActor

      log.info("Monitor Agent sends Report for " + applicationId)
      monitorMaster ! new ContainerReport(applicationId, null, null)
      monitorMaster.anchorPath
    }

  }

}
