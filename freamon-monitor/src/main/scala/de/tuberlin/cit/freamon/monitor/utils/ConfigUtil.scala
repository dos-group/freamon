package de.tuberlin.cit.freamon.monitor.utils

import java.io.{BufferedReader, FileReader, InputStreamReader}
import java.net.InetAddress

import com.typesafe.config.{Config, ConfigFactory}

object ConfigUtil {

  def loadHostConfig(args: Array[String]): Config = {
    if ((args.length == 2) && (args(0) == "-c")) {
      val path = args(1)
      println("Loading host configuration at " + path)
      val reader = new BufferedReader(new FileReader(path))
      ConfigFactory.parseReader(reader).withFallback(ConfigFactory.load()).resolve()
    } else {
      throw new IllegalArgumentException("No host config specified, use -c /path/to/myCluster.conf")
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
