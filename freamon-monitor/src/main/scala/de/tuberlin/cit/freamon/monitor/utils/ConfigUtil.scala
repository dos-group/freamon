package de.tuberlin.cit.freamon.monitor.utils

import java.io.{InputStreamReader, BufferedReader}
import java.net.InetAddress

import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config


object ConfigUtil {

  def loadHostConfig(): Config = {
    val hostName = InetAddress.getLocalHost.getHostName
    val reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/hosts/" + hostName + "/hosts.conf")))
    ConfigFactory.parseReader(reader).withFallback(ConfigFactory.load())
  }

  def setRemotingPort(config: Config, port: Int): Config = {
    val portConfig =
      ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port)
    portConfig.withFallback(config)
  }

}
