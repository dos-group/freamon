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

  def yarnPolling(intervalSec: Int) {
    val yclient: yarnClient = new yarnClient(hostConfig.getString("freamon.hosts.slaves.yarnsite"))
    // start polling
    val timer: Timer = new Timer
    System.out.println("Start polling yarn for applications every: " + intervalSec + " second(s)")
    timer.schedule(new TimerTask {
      override def run(): Unit = {
        val newApplicationId = yclient.evalYarnForNewApp()
        if (newApplicationId != null) {
          val containerIds = yclient.getApplicationContainerIds(newApplicationId)
          monitorMaster ! StartMonitoringForApplication(newApplicationId.toString, containerIds)
        }
        val releasedApplicationId = yclient.evalYarnForReleasedApp()
        if (releasedApplicationId != null) {
          monitorMaster ! StopMonitoringForApplication(releasedApplicationId.toString)
        }

      }
    }, 0, 1000 * intervalSec)
  }

  yarnPolling(3)

  //  Thread.sleep(10000)
  //  monitorMaster ! StartMonitoringForApplication("application_123", Array("container1", "container2"))

}
