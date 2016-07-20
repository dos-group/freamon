package de.tuberlin.cit.freamon.api

import java.lang.Double

case class PreviousRuns(scaleOuts: Array[Integer], runtimes: Array[Double])

case class StartMonitoringForApplication(applicationId: String, containerIds: Array[Long])

case class StopMonitoringForApplication(applicationId: String)

case class StartRecording(applicationId: String, containerIds: Array[String])

case class StopRecording(applicationId: String)

case class WorkerAnnouncement(workerHostname: String)

case class ContainerReport(applicationId: String, container: ContainerStats)
