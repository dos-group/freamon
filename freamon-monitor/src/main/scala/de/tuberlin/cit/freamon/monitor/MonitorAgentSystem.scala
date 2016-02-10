package de.tuberlin.cit.freamon.monitor

import java.io.File
import java.net.InetAddress

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object MonitorAgentSystem extends App {

  // build config for the actor system
  val hostName = InetAddress.getLocalHost.getHostName
  val file = new File(getClass().getResource("/hosts/" + hostName + "/hosts.conf").getFile)
  val hostConfig = ConfigFactory.parseFile(file).withFallback(ConfigFactory.load())
  val port = hostConfig.getInt("freamon.slaves.port")
  val portConfig =
    ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port)
  val systemConfig = portConfig.withFallback(ConfigFactory.load())

  val actorSystem = ActorSystem("agentSystem", systemConfig)

  val monitorAgent1 = actorSystem.actorOf(Props(new MonitorAgentActor()), name = "monitorAgent1")
  val monitorAgent2 = actorSystem.actorOf(Props(new MonitorAgentActor()), name = "monitorAgent2")

  monitorAgent1 ! startRecording("appId 1")

  monitorAgent1 ! sendReport("appId 1")
  monitorAgent2 ! sendReport("appId 2")
}
