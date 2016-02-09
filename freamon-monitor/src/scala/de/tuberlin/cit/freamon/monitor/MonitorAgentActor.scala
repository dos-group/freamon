package de.tuberlin.cit.freamon.monitor

import akka.actor.Actor
import akka.event.Logging
import de.tuberlin.cit.freamon.monitor.MonitorMasterActor.ContainerReport

case class sendReport(applicationId: String)

case class startRecording(applicationId: String)

class MonitorAgentActor() extends Actor {

  val log = Logging(context.system, this)

  def receive = {

    case sendReport(applicationId: String) => {

      val monitorMaster = context.actorSelection("akka.tcp://masterSystem@127.0.0.1:4512/user/monitorMaster")

      log.info("Monitor Agent sends Report for " + applicationId)
      monitorMaster ! new ContainerReport(applicationId, null, null)
      monitorMaster.anchorPath
    }

    case startRecording(applicationId: String) => {
      log.info("Monitor Agent starts recording for " + applicationId)
    }

  }

}
