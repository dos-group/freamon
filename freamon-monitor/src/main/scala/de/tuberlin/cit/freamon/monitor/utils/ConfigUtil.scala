package de.tuberlin.cit.freamon.monitor.utils

import java.io.{InputStreamReader, BufferedReader}
import java.net.InetAddress

import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config

object ConfigUtil {

  def loadHostConfig(hostName: String): Config = {
    println("Loading host configuration for " + hostName)
    val reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/hosts/" + hostName + "/hosts.conf")))
    ConfigFactory.parseReader(reader).withFallback(ConfigFactory.load())
  }

  def loadHostConfig(args: Array[String]): Config = {
    if ((args.length == 2) && (args(0) == "-h")) {
      loadHostConfig(args(1))
    } else {
      loadHostConfig(InetAddress.getLocalHost.getHostName)
    }
  }

  def setRemotingPort(config: Config, port: Int): Config = {
    val portConfig =
      ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port)
    portConfig.withFallback(config)
  }

  def setRemotingHost(config: Config, hostName: String): Config = {
    val hostNameConfig = ConfigFactory.parseString("akka.remote.netty.tcp.hostname=" + hostName)
    hostNameConfig.withFallback(config)
  }

}
