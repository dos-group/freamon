package de.tuberlin.cit.freamon.monitor

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object MonitorAgentSystem extends App {

  val actorSystem = ActorSystem("agentSystem", ConfigFactory.load().getConfig("agent"))

  val monitorAgent1 = actorSystem.actorOf(Props(new MonitorAgentActor()), name = "monitorAgent1")
  val monitorAgent2 = actorSystem.actorOf(Props(new MonitorAgentActor()), name = "monitorAgent2")

  monitorAgent1 ! startRecording("appId 1")

  monitorAgent1 ! sendReport("appId 1")
  monitorAgent2 ! sendReport("appId 2")
}
