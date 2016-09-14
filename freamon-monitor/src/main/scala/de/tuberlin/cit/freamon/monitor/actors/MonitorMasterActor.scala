package de.tuberlin.cit.freamon.monitor.actors

import java.lang.Double
import java.sql.SQLException
import java.time.Instant

import akka.actor.{Actor, ActorSelection, Address}
import akka.event.Logging
import de.tuberlin.cit.freamon.api._
import de.tuberlin.cit.freamon.results._
import de.tuberlin.cit.freamon.yarnclient.yarnClient
import org.apache.hadoop.yarn.api.records.ApplicationId
import de.tuberlin.cit.freamon.collector.AuditLogCollector

import scala.collection.mutable

case class StartProcessingAuditLog(path: String)

class MonitorMasterActor extends Actor {

  val log = Logging(context.system, this)
  var workers = mutable.Set[String]()
  val hostConfig = context.system.settings.config
  val yClient = new yarnClient
  var processAudit: Boolean = false

  // setup DB connection
  implicit val conn = DB.getConnection(
    hostConfig.getString("freamon.monetdb.url"),
    hostConfig.getString("freamon.monetdb.user"),
    hostConfig.getString("freamon.monetdb.password"))
  DB.createSchema()

  override def preStart(): Unit = {
    log.info("Monitor Master started")
  }

  def getAgentActorOnHost(hostname: String): ActorSelection = {
    val agentSystem = new Address("akka.tcp", hostConfig.getString("freamon.actors.systems.slave.name"),
      hostname, hostConfig.getInt("freamon.hosts.slaves.port"))
    val agentActorPath = agentSystem.toString + "/user/" + hostConfig.getString("freamon.actors.systems.slave.actor")
    context.system.actorSelection(agentActorPath)
  }

  def receive = {

    // we use arrays of Serializable so Freamon does not depend on yarn-workload-runner
    case msg @ ApplicationStart(applicationId, _, _, _, _) => {
      val splitAppId = applicationId.split("_")
      val clusterTimestamp = splitAppId(1).toLong
      val id = splitAppId(2).toInt
      val containerIds = yClient
          .getApplicationContainerIds(ApplicationId.newInstance(clusterTimestamp, id))
          .map(containerNr => {
            val attemptNr = 1 // TODO get from yarn, yarnClient assumes this to be 1
            val strippedAppId = applicationId.substring("application_".length)
            "container_%s_%02d_%06d".format(strippedAppId, attemptNr, containerNr)
          })

      for (host <- workers) {
        val agentActor = this.getAgentActorOnHost(host)
        agentActor ! StartRecording(applicationId, containerIds)
      }

      log.info(s"Job started: $applicationId at ${Instant.ofEpochMilli(msg.startTime)}")

      val job = new JobModel(applicationId, 'Flink, msg.signature,
        containerIds.length, msg.coresPerContainer, msg.memPerContainer, msg.startTime)
      try {
        JobModel.insert(job)
      } catch {
        case e: SQLException => log.error(s"Could not insert job $applicationId: ${e.getMessage}")
      }
    }

    case ApplicationStop(applicationId, stopTime) => {
      // TODO do not stop if already stopped

      for (host <- workers) {
        val agentActor = this.getAgentActorOnHost(host)
        agentActor ! StopRecording(applicationId)
        agentActor ! StopProcessingAuditLog
      }
      processAudit = false

      val oldJob: JobModel = JobModel.selectWhere(s"app_id = '$applicationId'").head

      val sec = (stopTime - oldJob.start) / 1000f
      log.info(s"Job stopped: $applicationId at ${Instant.ofEpochMilli(stopTime)}, took $sec seconds")

      JobModel.update(oldJob.copy(stop = stopTime))
    }

    case FindPreviousRuns(signature) => {
      val runs = JobModel.selectWhere(s"signature = '$signature'")
      sender ! PreviousRuns(
        runs.map(r => r.numContainers.asInstanceOf[Integer]).toArray,
        runs.map(r => ((r.stop - r.start) / 1000d).asInstanceOf[Double]).toArray
      )
    }

    case WorkerAnnouncement(workerHostname) => {
      log.info(sender + " registered a new worker on " + workerHostname)
      workers += workerHostname
    }

    case ContainerReport(applicationId, container) => {
      log.info("Received a container report of " + applicationId + " from " + sender)
      log.info("for container " + container.containerId + " with " + container.cpuUtil.length + " samples")
      log.debug("BlkIO: " + container.blkioUtil.mkString(", "))
      log.debug("CPU: " + container.cpuUtil.mkString(", "))
      log.debug("Net: " + container.netUtil.mkString(", "))
      log.debug("Memory: " + container.memUtil.mkString(", "))

      val job = JobModel.selectWhere(s"app_id = '$applicationId'").head
      val hostname = sender().path.address.hostPort
      val containerModel = ContainerModel(container.containerId, job.id, hostname)
      ContainerModel.insert(containerModel)

      val containerStart = job.start + 1000 * container.startTick
      for ((io, i) <- container.blkioUtil.zipWithIndex) {
        EventModel.insert(new EventModel(containerModel.id, job.id, 'blkio, containerStart + 1000 * i, io))
      }
      for ((cpu, i) <- container.cpuUtil.zipWithIndex) {
        EventModel.insert(new EventModel(containerModel.id, job.id, 'cpu, containerStart + 1000 * i, cpu))
      }
      for ((net, i) <- container.netUtil.zipWithIndex) {
        EventModel.insert(new EventModel(containerModel.id, job.id, 'net, containerStart + 1000 * i, net))
      }
      for ((mem, i) <- container.memUtil.zipWithIndex) {
        EventModel.insert(new EventModel(containerModel.id, job.id, 'mem, containerStart + 1000 * i, mem))
      }
    }

    case AuditLogSubmission(entry) => {
      log.info("Received an audit log entry from timestamp: "+entry.date)
      log.debug("Contents: allowed="+entry.allowed+", ugi="+entry.ugi+", ip="+entry.ip+
        ", cmd="+entry.cmd+", src="+entry.src+", dst="+entry.dst+", perm="+entry.perm+", proto="+entry.proto)
      AuditLogModel.insert(new AuditLogModel(entry.date, entry.allowed,
        entry.ugi, entry.ip, entry.cmd, entry.src, entry.dst,
        entry.perm, entry.proto))
    }

    case SerialAuditLogSubmission(entries) =>
      log.info("Received a series of audit log entries. There are "+entries.size()+" of them.")
      for(i <- 0 to entries.size()){
        AuditLogModel.insert(new AuditLogModel(entries.get(i).date, entries.get(i).allowed, entries.get(i).ugi,
          entries.get(i).ip, entries.get(i).cmd, entries.get(i).src, entries.get(i).dst, entries.get(i).perm, entries.get(i).proto))
      }
      log.info("Inserted "+entries.size+" entries to database.")

    case StartProcessingAuditLog(path) => {
      log.info("Starting to process the audit log...")
      processAudit = true
      AuditLogCollector.start(path)
      while (processAudit && AuditLogCollector.anyEntryStored){
        log.info("Requesting entries...")
        sender() ! SerialAuditLogSubmission(AuditLogCollector.getAllEntries)
      }
      log.info("Stopping requesting entries...")
    }

  }
}
