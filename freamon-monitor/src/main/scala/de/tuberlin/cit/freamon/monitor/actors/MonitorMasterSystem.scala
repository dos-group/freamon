package de.tuberlin.cit.freamon.monitor.actors

import java.util.{Timer, TimerTask}

import akka.actor.{ActorSystem, Props}
import de.tuberlin.cit.freamon.monitor.utils.ConfigUtil
import de.tuberlin.cit.freamon.yarnclient.yarnClient

object MonitorMasterSystem extends App {

  val hostConfig = ConfigUtil.loadHostConfig(args)
  val portConfig = ConfigUtil.setRemotingPort(hostConfig, hostConfig.getInt("freamon.hosts.master.port"))
  val masterConfig = ConfigUtil.setRemotingHost(portConfig, hostConfig.getString("freamon.hosts.master.hostname"))

  // start master system
  val actorSystem = ActorSystem(masterConfig.getString("freamon.actors.systems.master.name"), masterConfig)
  val monitorMasterName = masterConfig.getString("freamon.actors.systems.master.actor")
  val monitorMaster = actorSystem.actorOf(Props[MonitorMasterActor], name = monitorMasterName)

}
