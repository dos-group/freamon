package de.tuberlin.cit.freamon.monitor.actors

import java.net.InetAddress

import akka.actor.{ActorSystem, Props}

import de.tuberlin.cit.freamon.monitor.utils.ConfigUtil

object MonitorAgentSystem extends App {

  val hostConfig = ConfigUtil.loadHostConfig(args)
  val portConfig = ConfigUtil.setRemotingPort(hostConfig, hostConfig.getInt("freamon.hosts.slaves.port"))
  val agentConfig = ConfigUtil.setRemotingHost(portConfig, InetAddress.getLocalHost.getHostName)

  val actorSystem = ActorSystem(agentConfig.getString("freamon.actors.systems.slave.name"), agentConfig)

  val monitorAgent = actorSystem.actorOf(Props(new MonitorAgentActor()),
    name = agentConfig.getString("freamon.actors.systems.slave.actor"))

//  monitorAgent ! StartRecording("appId 1")
//  monitorAgent ! StopRecording("appId 1")
}
