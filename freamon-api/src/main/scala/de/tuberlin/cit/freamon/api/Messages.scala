package de.tuberlin.cit.freamon.api

import java.lang.Double

case class FindPreviousRuns(signature: String)

case class PreviousRuns(scaleOuts: Array[Integer], runtimes: Array[Double], datasetSizes: Array[Double])

case class ApplicationStart(applicationId: String, startTime: Long)

case class ApplicationStop(applicationId: String, stopTime: Long)

case class StartMonitoringForApplication(applicationId: String, containerIds: Array[Long])

case class StopMonitoringForApplication(applicationId: String)

case class StartRecording(applicationId: String, containerIds: Array[String])

case class StopRecording(applicationId: String)

case class WorkerAnnouncement(workerHostname: String)

case class ContainerReport(applicationId: String, containerId: String, samples: Array[StatSample])

case class ForwardAuditLogEntries()

case class ForwardedAuditLogEntry(entry: AuditLogEntry)

case class ApplicationMetadata(
                                appId: String,
                                framework: Symbol = Symbol(null),
                                signature: String = null,
                                inputSize: Double = 0d,
                                numWorkers: Int = 0,
                                coresPerWorker: Int = 0,
                                memoryPerWorker: Int = 0
                              )

/** Single data point of a resource's usage by a container, similar to freamon.results.EventModel
  *
  * @param kind   The event type.
  * @param millis The milliseconds after epoch for this event, as returned from System.currentTimeMillis().
  * @param value  The double value for this event.
  */
case class StatSample(
                       containerId: String,
                       kind: Symbol,
                       millis: Long,
                       value: Double
                     )
