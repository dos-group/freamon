package de.tuberlin.cit.freamon.monitor.actors

import java.lang.Double
import java.sql.SQLException
import java.time.Instant
import java.util.concurrent.LinkedBlockingQueue

import akka.actor.{Actor, ActorSelection, Address}
import akka.event.Logging
import de.tuberlin.cit.freamon.api._
import de.tuberlin.cit.freamon.collector.AuditLogProducer
import de.tuberlin.cit.freamon.monitor.utils.AuditLogForwarder
import de.tuberlin.cit.freamon.results._
import de.tuberlin.cit.freamon.yarnclient.yarnClient
import org.apache.hadoop.yarn.api.records.ApplicationId

import scala.collection.mutable

case class StartProcessingAuditLog(path: String)

class MonitorMasterActor extends Actor {

  val log = Logging(context.system, this)
  var workers = mutable.Set[String]()
  val hostConfig = context.system.settings.config
  val yClient = new yarnClient
  var processAudit: Boolean = false
  var queue: LinkedBlockingQueue[AuditLogEntry] = new LinkedBlockingQueue[AuditLogEntry]()
  var outwardQueue: LinkedBlockingQueue[AuditLogEntry] = new LinkedBlockingQueue[AuditLogEntry]()

  // setup DB connection
  implicit val conn = DB.getConnection(
    hostConfig.getString("freamon.monetdb.url"),
    hostConfig.getString("freamon.monetdb.user"),
    hostConfig.getString("freamon.monetdb.password"))
  DB.createSchema()

  override def preStart(): Unit = {
    log.info("Monitor Master started")
  }

  def makeYarnAppIdInstance(applicationId: String): ApplicationId = {
    val splitAppId = applicationId.split("_")
    val clusterTimestamp = splitAppId(1).toLong
    val id = splitAppId(2).toInt
    val yarnAppId = ApplicationId.newInstance(clusterTimestamp, id)
    yarnAppId
  }

  def getAgentActorOnHost(hostname: String): ActorSelection = {
    val agentSystem = new Address("akka.tcp", hostConfig.getString("freamon.actors.systems.slave.name"),
      hostname, hostConfig.getInt("freamon.hosts.slaves.port"))
    val agentActorPath = agentSystem.toString + "/user/" + hostConfig.getString("freamon.actors.systems.slave.actor")
    context.system.actorSelection(agentActorPath)
  }

    def startApplication(applicationId: String, startTime: Long) = {
      val containerIds = yClient
          .getApplicationContainerIds(makeYarnAppIdInstance(applicationId))
          .map(containerNr => {
            val attemptNr = 1 // TODO get from yarn, yarnClient assumes this to be 1
            val strippedAppId = applicationId.substring("application_".length)
            "container_%s_%02d_%06d".format(strippedAppId, attemptNr, containerNr)
          })

      for (host <- workers) {
        val agentActor = this.getAgentActorOnHost(host)
        agentActor ! StartRecording(applicationId, containerIds)
      }

      log.info(s"Job started: $applicationId at ${Instant.ofEpochMilli(startTime)}")

      val job = new JobModel(applicationId, start = startTime)
      try {
        JobModel.insert(job)
      } catch {
        case e: SQLException => log.error(s"Could not insert job $applicationId: ${e.getMessage}")
      }
    }

  def stopApplication(applicationId: String, stopTime: Long) = {
      // TODO do not stop if already stopped

      for (host <- workers) {
        val agentActor = this.getAgentActorOnHost(host)
        agentActor ! StopRecording(applicationId)
      }
      processAudit = false


      JobModel.selectWhere(s"yarn_application_id = '$applicationId'").headOption.map(oldJob => {
        val sec = (stopTime - oldJob.start) / 1000f
        log.info(s"Job finished: $applicationId at ${Instant.ofEpochMilli(stopTime)}, took $sec seconds")
        JobModel.update(oldJob.copy(stop = stopTime))
        Unit
      }).orElse({
        log.error("No such job in DB: " + applicationId + " (ApplicationStop)")
        None
      })
    }

  def receive = {

    case ApplicationStart(applicationId, stopTime) =>
      this.startApplication(applicationId, stopTime)
    case Array("jobStarted", applicationId: String, stopTime: Long) =>
      this.startApplication(applicationId, stopTime)

    case ApplicationStop(applicationId, stopTime) =>
      this.stopApplication(applicationId, stopTime)
    case Array("jobStopped", applicationId: String, stopTime: Long) =>
      this.stopApplication(applicationId, stopTime)

    case ApplicationMetadata(appId, framework, signature, inputSize, coresPC, memPC) => {
      JobModel.selectWhere(s"yarn_application_id = '$appId'").headOption.map(oldJob => {
        JobModel.update(oldJob.copy(appId,
          framework = framework,
          signature = signature,
          inputSize = inputSize,
          coresPerWorker = coresPC,
          memoryPerWorker = memPC))
        Unit
      }).getOrElse(log.warning("Cannot update application metadata for " + appId))
    }

    case FindPreviousRuns(signature) => {
      val runs = JobModel.selectWhere(s"signature = '$signature'")
      sender ! PreviousRuns(
        runs.map(_.numWorkers.asInstanceOf[Integer]).toArray,
        runs.map(r => ((r.stop - r.start) / 1000d).asInstanceOf[Double]).toArray,
        runs.map(_.inputSize.asInstanceOf[Double]).toArray
      )
    }

    case WorkerAnnouncement(workerHostname) => {
      log.info(sender + " registered a new worker on " + workerHostname)
      workers += workerHostname
    }

    case ContainerReport(applicationId, containerId, samples) =>
      log.info(s"Received a container report for $containerId from $sender")
      log.debug("with " + samples.length + " samples: " + samples.mkString(", "))

      JobModel.selectWhere(s"yarn_application_id = '$applicationId'").headOption match {
        case Some(job) =>
          val hostname = sender().path.address.hostPort
          val execUnit = ExecutionUnitModel.selectWhere(s"container_id = '$containerId'").headOption match {
            case Some(execUnit) => execUnit
            case None =>
              val execUnit = ExecutionUnitModel(job.id, hostname, isYarnContainer = true, containerId)
              ExecutionUnitModel.insert(execUnit)
              execUnit
          }

          for (foo <- samples) {
             EventModel.insert(new EventModel(execUnit.id, job.id, foo.kind, foo.millis, foo.value))
          }
        case None => log.error(s"No such job in DB: $applicationId (ContainerReport)")
      }

    case HostReport(workerId, samples) =>
      log.info(s"Received a general worker report from $workerId with "+samples.length+" samples.")

      for(sample <- samples){
        HostEventModel.insert(new HostEventModel(sample.containerId, sample.kind, sample.millis, sample.value))
        log.debug(s"Inserted: ${sample.containerId}, ${sample.kind}, ${sample.millis}, ${sample.value}\n")
      }

    case StartProcessingAuditLog(path) => {
      log.debug("Starting to process the audit log...")
      val producer: AuditLogProducer = new AuditLogProducer(queue, path)
      new Thread(producer).start()
      val forwarder: AuditLogForwarder = new AuditLogForwarder(queue, outwardQueue, conn)
      new Thread(forwarder).start()
    }

    case ForwardAuditLogEntries() => {
      log.debug("ForwardAuditLogEntries: Received a request for forwarding the AuditLogEntry objects to an external entity.")
      while (true){
        if(!outwardQueue.isEmpty){
          log.debug("ForwardAuditLogEntries: Outward queue not empty. Trying to forward an object")
          val ale: AuditLogEntry = outwardQueue.take()

          sender ! ForwardedAuditLogEntry(ale)
          log.debug("ForwardAuditLogEntries: Sent.")
        }
        else if (outwardQueue.isEmpty){
          log.debug("ForwardAuditLogEntries: Currently no new entries. Going to sleep for a second...")
          Thread.sleep(1000)
          log.debug("ForwardAuditLogEntries: Waked up. Trying again...")
        }
      }
    }
    case _ => {
      log.error("Received an unrecognised message")
    }
  }

  override def unhandled(message: Any): Unit={
      log.error("Caught an unknown message: "+message.toString)
  }

}
