package de.tuberlin.cit.freamon.monitor.actors

import akka.actor.{ActorRef, ActorSystem, Props}
import de.tuberlin.cit.freamon.api.{AuditLogEntry, AuditLogSubmission}
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
  monitorMaster.tell(StartProcessingAuditLog("/usr/local/hadoop/logs/hdfs-audit.log"), monitorMaster)

  def tellMasterMonitor(msg: Any): Unit ={
    monitorMaster.tell(msg, monitorMaster)
    println("told MasterMonitorActor")
  }

}
