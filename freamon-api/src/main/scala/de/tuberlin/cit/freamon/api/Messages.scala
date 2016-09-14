package de.tuberlin.cit.freamon.api

import java.lang.Double

case class FindPreviousRuns(signature: String)

case class PreviousRuns(scaleOuts: Array[Integer], runtimes: Array[Double])

case class ApplicationStart(applicationId: String, startTime: Long,
                            signature: String,
                            coresPerContainer: Int, memPerContainer: Int)

case class ApplicationStop(applicationId: String, stopTime: Long)

case class StartMonitoringForApplication(applicationId: String, containerIds: Array[Long])

case class StopMonitoringForApplication(applicationId: String)

case class StartRecording(applicationId: String, containerIds: Array[String])

case class StopRecording(applicationId: String)

case class WorkerAnnouncement(workerHostname: String)

case class ContainerReport(applicationId: String, container: ContainerStats)

case class AuditLogSubmission(entry: AuditLogEntry)

case class SerialAuditLogSubmission(entries: Array[AuditLogEntry])

case class StartProcessingAuditLog(path: String)

case class StopProcessingAuditLog(path: String)