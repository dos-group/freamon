package de.tuberlin.cit.freamon.monitor

import akka.actor.Actor
import akka.event.Logging
import de.tuberlin.cit.freamon.monitor.MonitorMasterActor.ContainerReport

object MonitorMasterActor {

  case class ContainerReport(containerId: String, cpuUtil: Array[Float], memUtil: Array[Int])

}

class MonitorMasterActor extends Actor {
  val log = Logging(context.system, this)

  override def preStart(): Unit ={
    log.info("Monitor Master started")

  }

  def receive = {
    case ContainerReport(containerId, cpuUtil, memUtil) => {
      log.info("recv a container Report of " + containerId)
    }
  }
}
