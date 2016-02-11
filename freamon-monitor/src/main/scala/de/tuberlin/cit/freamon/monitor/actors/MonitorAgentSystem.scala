package de.tuberlin.cit.freamon.monitor.actors

import akka.actor.{ActorSystem, Props}

import de.tuberlin.cit.freamon.monitor.utils.ConfigUtil

object MonitorAgentSystem extends App {

  val hostConfig = ConfigUtil.loadHostConfig()
  val agentConfig = ConfigUtil.setRemotingPort(hostConfig, hostConfig.getInt("freamon.hosts.slaves.port"))

  val actorSystem = ActorSystem(agentConfig.getString("freamon.actors.systems.slave.name"), agentConfig)

  val monitorAgent1 = actorSystem.actorOf(Props(new MonitorAgentActor()), name = "monitorAgent1")
  val monitorAgent2 = actorSystem.actorOf(Props(new MonitorAgentActor()), name = "monitorAgent2")

  monitorAgent1 ! startRecording("appId 1")

  monitorAgent1 ! sendReport("appId 1")
  monitorAgent2 ! sendReport("appId 2")
}
