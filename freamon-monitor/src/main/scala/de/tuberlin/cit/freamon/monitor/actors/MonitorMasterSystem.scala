package de.tuberlin.cit.freamon.monitor.actors

import akka.actor.{ActorSystem, Props}

import de.tuberlin.cit.freamon.monitor.utils.ConfigUtil

object MonitorMasterSystem extends App {

  val hostConfig = ConfigUtil.loadHostConfig()
  val masterConfig = ConfigUtil.setRemotingPort(hostConfig, hostConfig.getInt("freamon.hosts.master.port"))

  // start master system
  val actorSystem = ActorSystem(masterConfig.getString("freamon.actors.systems.master.name"), masterConfig)
  val monitorMasterName = masterConfig.getString("freamon.actors.systems.master.actor")
  val monitorMaster = actorSystem.actorOf(Props[MonitorMasterActor], name = monitorMasterName)

}
