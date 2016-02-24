package de.tuberlin.cit.freamon.monitor.utils

import java.io.{BufferedReader, FileReader, InputStreamReader}
import java.net.InetAddress

import com.typesafe.config.{Config, ConfigFactory}

object ConfigUtil {

  def loadHostConfig(hostName: String): Config = {
    println("Loading host configuration for " + hostName)
    val stream =
      try new InputStreamReader(getClass().getResourceAsStream("/hosts/" + hostName + "/hosts.conf"))
      catch { // not found in jar, try filesystem
        case _: NullPointerException => new FileReader("/hosts/" + hostName + "/hosts.conf")
      }
    ConfigFactory.parseReader(new BufferedReader(stream)).withFallback(ConfigFactory.load())
  }

  def loadHostConfig(args: Array[String]): Config = {
    if ((args.length == 2) && (args(0) == "-h")) {
      loadHostConfig(args(1))
    } else {
      loadHostConfig(InetAddress.getLocalHost.getHostName)
    }
  }

  def setRemotingPort(config: Config, port: Int): Config = {
    val portConfig = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port)
    portConfig.withFallback(config)
  }

  def setRemotingHost(config: Config, hostName: String): Config = {
    val hostNameConfig = ConfigFactory.parseString("akka.remote.netty.tcp.hostname=" + hostName)
    hostNameConfig.withFallback(config)
  }

}
