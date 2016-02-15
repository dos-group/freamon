package de.tuberlin.cit.freamon.collector

import java.io.IOException
import java.util.concurrent.{Executors, TimeUnit}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
 * Stores the recorded statistics about one container.
 *
 * @param containerId ID of the monitored container
 * @param startTick ticks after application start when this container was started to be monitored
 */
case class ContainerStats(containerId: Long, startTick: Long) {
  val cpuUtil = new ArrayBuffer[Float]
  val memUtil = new ArrayBuffer[Int]
}

object AppStatsCollector {

  def main(args: Array[String]) {
    if (args.length < 3) {
      println("Params: </path/to/yarn-site.xml> <seconds to run> <appId> [containerID ...]")
      return
    }

    val yarnSitePath = args(0)
    val secondsToRun = args(1).toInt
    val appId = args(2)
    val containerIds = args.drop(3).map(_.toLong)
    println("Starting for " + secondsToRun + "s with YARN config at " + yarnSitePath)
    println("Recording " + containerIds.length + " containers: " + containerIds.mkString(" "))

    val appStats = new AppStatsCollector(appId, new YarnConfig(yarnSitePath), 1)
    appStats.addContainers(containerIds)
    appStats.onCollect = container => {
      println(container.containerId + " CPU-avg: " + container.cpuUtil.last + " cores")
      println(container.containerId + " Memory: " + container.memUtil.last + " MB")
    }

    appStats.startRecording()
    Thread.sleep(1000 * secondsToRun)
    val results = appStats.stopRecording()

    for (container <- results) {
      println(container.containerId + " CPU history: " + container.cpuUtil.mkString(", "))
      println(container.containerId + " Memory history: " + container.memUtil.mkString(", "))
    }
  }
}

/**
 * Collects statistics from a single node.
 */
class AppStatsCollector(applicationId: String, yarnConfig: YarnConfig, intervalSeconds: Long) {

  private val executor = Executors.newScheduledThreadPool(1)

  val containerCgroups = new mutable.HashMap[Long, Cgroup]()
  val containerStats = new ArrayBuffer[ContainerStats]()
  var ticksPassed = 0

  var onCollect = (container: ContainerStats) => {}


  /**
   * Try adding a set of containers to monitor.
   *
   * @return this instance for method chaining
   */
  def addContainers(containerIds: Array[Long]) = {
    // applicationId format: application_1455551433868_0002
    val strippedAppId = applicationId.substring("application_".length)

    for (containerId <- containerIds) {
      try {
        val attemptNr = 1 // TODO get from yarn, yarnClient assumes this to be 1
        val fullId = "container_%s_%02d_%06d".format(strippedAppId, attemptNr, containerId)
        val cgroup = new Cgroup(yarnConfig.cgroupsMountPath, yarnConfig.cgroupsHierarchy + "/" + fullId)

        containerCgroups.put(containerId, cgroup)
        containerStats += new ContainerStats(containerId, ticksPassed)
      }
      catch {
        case e: IOException => // skip this container, it is not on this node
      }
    }

    this
  }

  /**
   * Starts recording after clearing all previous statistics.
   */
  def startRecording() = {

    def tryOrElse[T](f: => T, default: T): T = {
      try f
      catch {
        case e: Throwable =>
          println("AppStatsCollector encountered an error during stats collection for " + applicationId)
          e.printStackTrace()
          default
      }
    }

    val runnable = new Runnable() {
      def run() {
        // TODO lock
        ticksPassed += 1
        for (container <- containerStats) {
          val cgroup = containerCgroups(container.containerId)

          container.cpuUtil += tryOrElse(cgroup.getCurrentCpuUsage, Float.NaN)
          // TODO memory might not be managed using cgroups, use other source
          container.memUtil += tryOrElse((cgroup.getCurrentMemUsage / 1024 / 1024).toInt, -1)

          onCollect(container)
        }
      }
    }

    // TODO decide if several updates in quick succession or delayed values are better; if the latter, use Timer.schedule
    executor.scheduleAtFixedRate(runnable, 0, intervalSeconds * 1000, TimeUnit.MILLISECONDS)
  }

  /**
   * Stop recording and return all recorded container statistics.
   */
  def stopRecording(): Array[ContainerStats] = {
    executor.shutdownNow

    val results = containerStats.toArray
    containerStats.clear()
    containerCgroups.clear()

    results
  }

}
