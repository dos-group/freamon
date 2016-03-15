package de.tuberlin.cit.freamon.monitor.actors

import java.time.Instant

import akka.actor.{Actor, ActorSelection, Address}
import akka.event.Logging
import de.tuberlin.cit.freamon.collector.ContainerStats
import de.tuberlin.cit.freamon.results.{EventModel, JobModel, DB}

import scala.collection.mutable

case class StartMonitoringForApplication(applicationId: String, containerIds: Array[Long])

case class StopMonitoringForApplication(applicationId: String)

case class WorkerAnnouncement(workerHostname: String)

case class ContainerReport(applicationId: String, container: ContainerStats)


class MonitorMasterActor extends Actor {

  val log = Logging(context.system, this)
  var workers: scala.collection.mutable.ListBuffer[String] = mutable.ListBuffer()
  val hostConfig = context.system.settings.config

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

    case StartMonitoringForApplication(applicationId: String, containerIds: Array[Long]) => {
      val now: Instant = Instant.now()
      for (host <- workers) {
        val agentActor = this.getAgentActorOnHost(host)
        agentActor ! StartRecording(applicationId, containerIds)
      }
      JobModel.insert(new JobModel(applicationId, 'Generic, containerIds.length, -1, -1, now))
      // TODO ContainerModel
    }

    case StopMonitoringForApplication(applicationId: String) => {
      val now: Instant = Instant.now()
      for (host <- workers) {
        val agentActor = this.getAgentActorOnHost(host)
        agentActor ! StopRecording(applicationId)
      }
      val oldJob: JobModel = JobModel.selectWhere(s"app_id = '$applicationId'").head
      JobModel.update(oldJob.copy(stop = now))
    }

    case WorkerAnnouncement(workerHostname) => {
      log.info(sender + " registered a new worker on " + workerHostname)
      workers += workerHostname
    }

    case ContainerReport(applicationId, container) => {
      log.info("Received a container report of " + applicationId + " from " + sender)
      log.info("for container " + container.containerId + " with " + container.cpuUtil.length + " samples:")
      log.info("CPU: " + container.cpuUtil.mkString(", "))
      log.info("Memory: " + container.memUtil.mkString(", "))
      val job: JobModel = JobModel.selectWhere(s"app_id = '$applicationId'").head
      val containerStart: Instant = job.start.plusSeconds(container.startTick)
      for ((cpu, i) <- container.cpuUtil.zipWithIndex) {
        EventModel.insert(new EventModel(job.id, 'cpu, containerStart.plusSeconds(i), cpu))
      }
    }

  }
}
