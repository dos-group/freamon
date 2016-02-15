package de.tuberlin.cit.freamon.collector

import java.io.IOException
import java.util.concurrent.{Executors, TimeUnit}

import scala.collection.mutable.ArrayBuffer

object AppStatsCollector {

  def main(args: Array[String]) {
    if (args.length < 3) {
      println("Params: </path/to/yarn-site.xml> <seconds to run> <appId> [containerID ...]")
      return
    }

    val yarnSitePath = args(0)
    val seconds = args(1).toInt
    val appId = args(2)
    val containerIds = args.drop(3).map(_.toLong)
    println("Starting for " + seconds + "s with YARN config at " + yarnSitePath)
    println("Recording " + containerIds.length + " containers: " + containerIds.mkString(" "))

    val appStats = new AppStatsCollector(appId, containerIds, yarnSitePath, 1)
    appStats.onCollect = () => {
      println("CPU-avg: " + appStats.cpuUtil.last + " cores")
      println("Memory: " + appStats.memUtil.last + " MB")
    }

    appStats.startRecording()
    Thread.sleep(1000 * seconds)
    val (cpuUtil, memUtil) = appStats.stopRecording()

    println("CPU history: " + cpuUtil.mkString(", "))
    println("Memory history: " + memUtil.mkString(", "))
  }
}

/**
 * Collects statistics from a single node.
 */
class AppStatsCollector(applicationId: String, containerIds: Array[Long], yarnSitePath: String, intervalSeconds: Long) {

  private val executor = Executors.newScheduledThreadPool(1)
  private val conf = new YarnConfig(yarnSitePath)

  val cpuUtil = new ArrayBuffer[Float]
  val memUtil = new ArrayBuffer[Int]
  // TODO also store per-container stats

  var onCollect = () => {}

  /**
   * Starts recording after clearing all previous statistics.
   *
   * @return this instance for method chaining
   */
  def startRecording() = {
    val containers = containerIds.map(containerId => {
      val attemptNr = 1 // TODO get from yarn, yarn-client assumes this to be 1
      val fullId = "container_%s_%02d_%06d".format(applicationId, attemptNr, containerId)

      try new Cgroup(conf.cgroupsMountPath, conf.cgroupsHierarchy + "/" + fullId)
      catch {
        case e: IOException =>
          throw new IOException("AppStatsCollector for " + applicationId + " could not read cgroup for container " + fullId, e)
      }
    }).toList

    cpuUtil.clear()
    memUtil.clear()

    val runnable = new Runnable() {
      def run() {
        try {
          val containerCpus = containers.map(_.getCurrentCpuUsage)
          val containerMems = containers.map(_.getCurrentMemUsage / 1024 / 1024).map(_.toInt)

          cpuUtil += containerCpus.sum
          memUtil += containerMems.sum

          onCollect()
        }
        catch {
          case e: Throwable =>
            // print error to stdout, or it will get discarded by the executor
            println("AppStatsCollector for " + applicationId + " encountered an error during stats collection")
            e.printStackTrace()
            throw e // stop the executor
        }
      }
    }

    // TODO decide if several updates in quick succession or delayed values are better; if the latter, use Timer.schedule
    executor.scheduleAtFixedRate(runnable, 0, intervalSeconds * 1000, TimeUnit.MILLISECONDS)

    this
  }

  /**
   * Stops recording and returns all collected statistics.
   */
  def stopRecording() = {
    executor.shutdownNow

    (cpuUtil.toArray, memUtil.toArray)
  }

}
