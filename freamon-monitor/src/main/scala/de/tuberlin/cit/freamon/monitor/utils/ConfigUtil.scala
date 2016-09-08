package de.tuberlin.cit.freamon.monitor.utils

import java.io.{BufferedReader, FileReader}

import com.typesafe.config.{Config, ConfigFactory}

object ConfigUtil {

  def loadClusterConfig(args: Array[String]): Config = {
    if ((args.length == 2) && (args(0) == "-c")) {
      val path = args(1)
      println("Loading cluster configuration at " + path)
      val reader = new BufferedReader(new FileReader(path))
      ConfigFactory.parseReader(reader).withFallback(ConfigFactory.load()).resolve()
    } else {
      throw new IllegalArgumentException("No cluster config specified, use -c /path/to/myCluster.conf")
    }
  }

  def readAuditLog(args: Array[String]): Unit = {
    AuditLogManager.receiveRequest(args)
  }

  def setRemotingHostPort(config: Config, hostName: String, port: Int): Config = {
    ConfigFactory.parseString(
      "akka.remote.netty.tcp.hostname=" + hostName
        + "\nakka.remote.netty.tcp.port=" + port
    ).withFallback(config)
  }
}
