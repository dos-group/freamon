package de.tuberlin.cit.freamon.monitor

import java.net.InetAddress
import java.io.File

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object MonitorMasterSystem extends App {

  // build config for the actor system
  val hostName = InetAddress.getLocalHost.getHostName
  val file = new File(getClass().getResource("/hosts/" + hostName + "/hosts.conf").getFile)
  val hostConfig = ConfigFactory.parseFile(file).withFallback(ConfigFactory.load())
  val port = hostConfig.getInt("freamon.master.port")
  val portConfig =
    ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port)
  val systemConfig = portConfig.withFallback(ConfigFactory.load())

  // start master system
  val actorSystem = ActorSystem("masterSystem", systemConfig)
  val monitorMaster = actorSystem.actorOf(Props[MonitorMasterActor], name = "monitorMaster")

}
