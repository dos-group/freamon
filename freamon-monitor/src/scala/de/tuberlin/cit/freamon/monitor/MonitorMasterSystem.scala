package de.tuberlin.cit.freamon.monitor

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object MonitorMasterSystem extends App {

  val actorSystem = ActorSystem("masterSystem", ConfigFactory.load().getConfig("master"))

  val monitorMaster = actorSystem.actorOf(Props[MonitorMasterActor], name = "monitorMaster")

}
