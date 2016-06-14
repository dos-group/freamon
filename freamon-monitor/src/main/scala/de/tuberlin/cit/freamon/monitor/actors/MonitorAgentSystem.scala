package de.tuberlin.cit.freamon.monitor.actors

import java.net.InetAddress

import akka.actor.{ActorSystem, Props}
import de.tuberlin.cit.freamon.monitor.utils.ConfigUtil

object MonitorAgentSystem extends App {

  val clusterConfig = ConfigUtil.loadClusterConfig(args)
  val agentConfig = ConfigUtil.setRemotingHostPort(clusterConfig,
    InetAddress.getLocalHost.getHostName,
    clusterConfig.getInt("freamon.hosts.slaves.port"))

  val actorSystem = ActorSystem(agentConfig.getString("freamon.actors.systems.slave.name"), agentConfig)

  val monitorAgent = actorSystem.actorOf(Props(new MonitorAgentActor),
    name = agentConfig.getString("freamon.actors.systems.slave.actor"))

}
