package de.tuberlin.cit.freamon.monitor.actors

import akka.actor.{Address, Actor}
import akka.event.Logging

import de.tuberlin.cit.freamon.monitor.actors.MonitorMasterActor.ContainerReport

case class sendReport(applicationId: String)

case class startRecording(applicationId: String)

class MonitorAgentActor() extends Actor {

  val log = Logging(context.system, this)

  def receive = {

    case sendReport(applicationId: String) => {

      val hostConfig = context.system.settings.config

      val masterSystemPath = new Address("akka.tcp", hostConfig.getString("freamon.actors.systems.master.name"),
        hostConfig.getString("freamon.hosts.master.host"), hostConfig.getInt("freamon.hosts.master.port"))

      val masterActorPath = masterSystemPath.toString + "/user/" + hostConfig.getString("freamon.actors.systems.master.actor")

      val monitorMaster = context.actorSelection(masterActorPath)

      log.info("Monitor Agent sends Report for " + applicationId)
      monitorMaster ! new ContainerReport(applicationId, null, null)
      monitorMaster.anchorPath
    }

    case startRecording(applicationId: String) => {
      log.info("Monitor Agent starts recording for " + applicationId)
    }

  }

}
