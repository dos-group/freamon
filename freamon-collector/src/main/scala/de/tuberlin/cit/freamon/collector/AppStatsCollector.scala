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

    // try to create cgroup monitors first, as this might fail
    containerCgroups ++= containerIds.map(containerId => {
      val attemptNr = 1 // TODO get from yarn, yarnClient assumes this to be 1
      val fullId = "container_%s_%02d_%06d".format(strippedAppId, attemptNr, containerId)

      try {
        val cgroup = new Cgroup(yarnConfig.cgroupsMountPath, yarnConfig.cgroupsHierarchy + "/" + fullId)

        (containerId, cgroup)
      }
      catch {
        case e: IOException =>
          // TODO skip this container, it is on a different node
          throw new IOException("AppStatsCollector for " + applicationId + " could not read cgroup for container " + fullId, e)
      }
    })

    containerStats ++= containerIds.map(new ContainerStats(_, ticksPassed))

  this
  }

  /**
   * Starts recording after clearing all previous statistics.
   */
  def startRecording() = {
    val runnable = new Runnable() {
      def run() {
        // TODO lock
        ticksPassed += 1
        for (stats <- containerStats) {
          val cgroup = containerCgroups(stats.containerId)
          try {
            val containerCpus = cgroup.getCurrentCpuUsage
            // TODO this is optional
            val containerMems = (cgroup.getCurrentMemUsage / 1024 / 1024).toInt

            stats.cpuUtil += containerCpus
            stats.memUtil += containerMems

            onCollect(stats)
          }
          catch {
            case e: Throwable =>
              // print error to stdout, or it will get discarded by the executor
              println("AppStatsCollector encountered an error during stats collection for " + stats.containerId)
              e.printStackTrace()
          }
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
