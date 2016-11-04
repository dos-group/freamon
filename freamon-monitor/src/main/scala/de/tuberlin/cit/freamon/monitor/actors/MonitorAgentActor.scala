package de.tuberlin.cit.freamon.monitor.actors

import java.net.InetAddress

import akka.actor.{Actor, ActorSelection, Address}
import akka.event.Logging
import com.typesafe.config.Config
import de.tuberlin.cit.freamon.api._
import de.tuberlin.cit.freamon.collector.{NetHogsMonitor, NetUsageSample, ProcPoll}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class MonitorAgentActor() extends Actor {

  val log = Logging(context.system, this)
  val hostConfig = context.system.settings.config

  // appId -> containerId
  val containersPerApp = new mutable.HashMap[String, ArrayBuffer[String]]

  // containerId -> [sample]
  val containerStats = new mutable.HashMap[String, ArrayBuffer[StatSample]]

  // required to find the container's pid once a container starts
  val procPoll = new ProcPoll(1, self ! _).startRecording()

  val netHogsMonitor = {
    val netHogsCommandConfigKey = "freamon.hosts.slaves.nethogsCommand"
    if (hostConfig.hasPath(netHogsCommandConfigKey) && !hostConfig.getIsNull(netHogsCommandConfigKey)) {
      val nethogsCommand = hostConfig.getString(netHogsCommandConfigKey)
      Some(new NetHogsMonitor(nethogsCommand, self ! _))
    } else {
      None
    }
  }

  override def preStart(): Unit = {
    log.info("Monitor Agent started")

    this.getMasterActor(hostConfig) ! WorkerAnnouncement(InetAddress.getLocalHost.getHostName)
  }

  def getMasterActor(hostConfig: Config): ActorSelection = {
    val masterSystemPath = new Address("akka.tcp",
      hostConfig.getString("freamon.actors.systems.master.name"),
      hostConfig.getString("freamon.hosts.master.hostname"),
      hostConfig.getInt("freamon.hosts.master.port"))

    val masterActorPath = masterSystemPath.toString + "/user/" + hostConfig.getString("freamon.actors.systems.master.actor")

    context.actorSelection(masterActorPath)
  }

  def receive = {

    case StartRecording(applicationId: String, containerIds: Array[String]) =>
      log.info("Monitor Agent starts recording for app " + applicationId)
      log.info("Requested " + containerIds.length
        + " containers: " + containerIds.mkString(", "))

      containersPerApp.get(applicationId) match {
        case Some(appContainers) => appContainers ++= containerIds
        case None =>
          val appContainers = new ArrayBuffer[String]()
          containersPerApp.put(applicationId, appContainers)
          appContainers ++= containerIds
          for (containerId <- containerIds) {
            containerStats.put(containerId, new ArrayBuffer[StatSample]())
          }
      }

      procPoll.addContainers(containerIds)

    case StopRecording(applicationId: String) =>
      containersPerApp.remove(applicationId) match {
        case None => log.warning("Monitor Agent has no reports for " + applicationId)
        case Some(appContainers) =>
          log.info(s"Monitor Agent sends reports for $applicationId")
          for (containerId <- appContainers) {
            containerStats.remove(containerId) foreach { samples =>
              log.debug(s"${samples.length} samples for $containerId")
              if (samples.nonEmpty)
                sender ! ContainerReport(applicationId, containerId, samples.toArray)
            }
          }
      }

    case sample: StatSample =>
      containerStats get sample.containerId foreach { samples =>
        log.debug(s"Recording sample $sample")
        samples += sample
      }

    case sample: NetUsageSample =>
      // store sample if pid is being recorded (is key in hashmap)
      procPoll.containersByPid.get(sample.pid) match {
        case None => // ignored pid
        case Some(containerId) =>
          containerStats get containerId map { stats =>
            log.debug(s"Recording net sample $sample")
            stats += StatSample(containerId, 'netTx, sample.time, sample.transmit.asInstanceOf[Double])
            stats += StatSample(containerId, 'netRx, sample.time, sample.receive.asInstanceOf[Double])
          }
      }

  }

}
