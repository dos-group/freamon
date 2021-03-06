package de.tuberlin.cit.freamon.monitor.actors

import java.net.InetAddress

import akka.actor.{Actor, ActorSelection, Address}
import akka.event.Logging
import com.typesafe.config.Config
import de.tuberlin.cit.freamon.api._
import de.tuberlin.cit.freamon.collector._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class MonitorAgentActor() extends Actor {

  val log = Logging(context.system, this)

  val hostConfig = context.system.settings.config

  // how many samples to queue up (in containerStats) before sending a message to the master
  val sampleBatchSize = hostConfig.getInt("freamon.hosts.slaves.batchsize")

  // appId -> containerId
  val containersPerApp = new mutable.HashMap[String, ArrayBuffer[String]]

  // containerId -> [sample]
  val containerStats = new mutable.HashMap[String, ArrayBuffer[StatSample]]

  // hostname -> [sample]
  val hostStats = new mutable.HashMap[String, ArrayBuffer[StatSample]]

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

  val dstatMonitor = {
    val dstatConfigKey = "freamon.hosts.slaves.hostRecording"
    if (hostConfig.hasPath(dstatConfigKey) && !hostConfig.getIsNull(dstatConfigKey)) {
      val dstatRecording = hostConfig.getBoolean(dstatConfigKey)
      if (dstatRecording)
        Some(new DstatMonitor(self ! _))
      else None
    }
    hostStats.put(InetAddress.getLocalHost.getHostName, new ArrayBuffer[StatSample]())
  }

  val pidStatMonitor = {
    val pidStatCommandConfigKey = "freamon.hosts.slaves.pidstatCommand"
    if (hostConfig.hasPath(pidStatCommandConfigKey) && !hostConfig.getIsNull(pidStatCommandConfigKey)) {
      val pidstatCommand = hostConfig.getString(pidStatCommandConfigKey)
      Some(new PidStatMonitor(pidstatCommand, self ! _))
    } else {
      None
    }
  }

  val masterActor: ActorSelection = this.getMasterActor(hostConfig)

  override def preStart(): Unit = {
    log.info("Monitor Agent started")

    masterActor ! WorkerAnnouncement(InetAddress.getLocalHost.getHostName)
  }

  def getMasterActor(hostConfig: Config): ActorSelection = {
    val masterSystemPath = new Address("akka.tcp",
      hostConfig.getString("freamon.actors.systems.master.name"),
      hostConfig.getString("freamon.hosts.master.hostname"),
      hostConfig.getInt("freamon.hosts.master.port"))

    val masterActorPath = masterSystemPath.toString + "/user/" + hostConfig.getString("freamon.actors.systems.master.actor")

    context.actorSelection(masterActorPath)
  }

  def recordSample(sample: StatSample): Unit = {
    containerStats get sample.containerId foreach { samples =>
      log.debug(s"Recording sample $sample")
      samples += sample
      if (samples.size >= sampleBatchSize) {
        // the container id string contains the app id if it's a yarn container,
        // but getting it this way will work in any case
        val appId = containersPerApp.filter({
          case (_, containerIds) => containerIds.contains(sample.containerId)
        }).head._1
        log.debug(s"Sending collected samples for ${sample.containerId}")
        masterActor ! ContainerReport(appId, sample.containerId, samples.toArray)
        samples.clear()
      }
    }
  }

  def recordHostSample(sample: StatSample): Unit = {
    // Don't get confused about containerId: It's just reusing the same data type
    hostStats get sample.containerId foreach { samples =>
      log.debug(s"Recording general sample $sample")
      samples += sample
      if (samples.size >= sampleBatchSize) {
        log.debug(s"Sending collected samples for ${sample.containerId}")
        masterActor ! HostReport(sample.containerId, samples.toArray)
        samples.clear()
      }
    }
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
      recordSample(sample)

    case sample: NetUsageSample =>
      // store sample if pid is being recorded (is key in hashmap)
      procPoll.containersByPid.get(sample.pid) match {
        case None => // ignored pid
        case Some(containerId) =>
          log.debug(s"Recording net sample $sample")
          recordSample(StatSample(containerId, 'netTx, sample.time, sample.transmit))
          recordSample(StatSample(containerId, 'netRx, sample.time, sample.receive))
      }

    case sample: PidStatSample =>
      // store sample if pid is being recorded (is key in hashmap)
      procPoll.containersByPid.get(sample.pid) match {
        case None => // ignored pid
        case Some(containerId) =>
          log.debug(s"Recording pidstat sample $sample")
          recordSample(StatSample(containerId, 'cpuUsr, sample.time, sample.usr))
          recordSample(StatSample(containerId, 'cpuSys, sample.time, sample.sys))
          recordSample(StatSample(containerId, 'diskRd, sample.time, sample.read))
          recordSample(StatSample(containerId, 'diskWr, sample.time, sample.write))
      }

    case sample: HostSample =>
      // store sample if slave records its overall state
      log.debug(s"Recording dstat sample $sample")
      recordHostSample(StatSample(InetAddress.getLocalHost.getHostName, 'cpuUsr, sample.time, sample.cpuUsr))
      recordHostSample(StatSample(InetAddress.getLocalHost.getHostName, 'cpuSys, sample.time, sample.cpuSys))
      recordHostSample(StatSample(InetAddress.getLocalHost.getHostName, 'cpuIdl, sample.time, sample.cpuIdl))
      recordHostSample(StatSample(InetAddress.getLocalHost.getHostName, 'cpuWai, sample.time, sample.cpuWai))
      recordHostSample(StatSample(InetAddress.getLocalHost.getHostName, 'diskRd, sample.time, sample.diskRd))
      recordHostSample(StatSample(InetAddress.getLocalHost.getHostName, 'diskWr, sample.time, sample.diskWr))
      recordHostSample(StatSample(InetAddress.getLocalHost.getHostName, 'netRx, sample.time, sample.netRx))
      recordHostSample(StatSample(InetAddress.getLocalHost.getHostName, 'netTx, sample.time, sample.netTx))
      recordHostSample(StatSample(InetAddress.getLocalHost.getHostName, 'mem, sample.time, sample.mem))

  }

}
