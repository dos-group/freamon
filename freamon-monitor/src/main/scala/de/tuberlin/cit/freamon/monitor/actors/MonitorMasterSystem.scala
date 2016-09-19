package de.tuberlin.cit.freamon.monitor.actors

import akka.actor.{ActorSystem, Props}
import de.tuberlin.cit.freamon.monitor.utils.ConfigUtil

object MonitorMasterSystem extends App {

  val clusterConfig = ConfigUtil.loadClusterConfig(args)
  val masterConfig = ConfigUtil.setRemotingHostPort(clusterConfig,
    clusterConfig.getString("freamon.hosts.master.hostname"),
    clusterConfig.getInt("freamon.hosts.master.port"))

  // start master system
  val actorSystem = ActorSystem(masterConfig.getString("freamon.actors.systems.master.name"), masterConfig)
  val monitorMasterName = masterConfig.getString("freamon.actors.systems.master.actor")
  val monitorMaster = actorSystem.actorOf(Props[MonitorMasterActor], name = monitorMasterName)
  if (!clusterConfig.getIsNull("freamon.hosts.master.pathToAuditLog"))
    monitorMaster.tell(StartProcessingAuditLog(clusterConfig.getString("freamon.hosts.master.pathToAuditLog")), monitorMaster)

}
