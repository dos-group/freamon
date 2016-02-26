package de.tuberlin.cit.freamon.collector

import java.io.{FileInputStream, IOException}
import java.util.concurrent.{Executors, TimeUnit}

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.yarn.conf.YarnConfiguration
import org.apache.hadoop.yarn.conf.YarnConfiguration._

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

    val yarnConf = new YarnConfiguration()
    yarnConf.addResource(new FileInputStream(yarnSitePath), YARN_SITE_CONFIGURATION_FILE)

    val appStats = new AppStatsCollector(appId, yarnConf, 1)
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
class AppStatsCollector(applicationId: String, yarnConfig: Configuration, intervalSeconds: Long) {

  private val executor = Executors.newScheduledThreadPool(1)

  // applicationId format: application_1455551433868_0002
  val strippedAppId = applicationId.substring("application_".length)

  val cgroupsMountPath = yarnConfig.get(NM_LINUX_CONTAINER_CGROUPS_MOUNT_PATH, "/sys/fs/cgroup/")
  val cgroupsHierarchy = yarnConfig.get(NM_LINUX_CONTAINER_CGROUPS_HIERARCHY, "/hadoop-yarn")

  val containerIds = new mutable.HashSet[Long]()
  val containerCgroups = new mutable.HashMap[Long, Cgroup]()
  val containerStats = new ArrayBuffer[ContainerStats]()
  var ticksPassed = 0

  var onCollect = (container: ContainerStats) => {}

  def addContainers(newContainerIds: Array[Long]) = {
    containerIds ++= newContainerIds
    this
  }

  def startRecording() = {

    val runnable = new Runnable() {
      def run() {
        ticksPassed += 1

        // try to create previously unavailable container monitors
        for (containerId <- containerIds) {
          containerCgroups.getOrElse(containerId, try {
            val attemptNr = 1 // TODO get from yarn, yarnClient assumes this to be 1
            val fullId = "container_%s_%02d_%06d".format(strippedAppId, attemptNr, containerId)
            val cgroup = new Cgroup(cgroupsMountPath, cgroupsHierarchy + "/" + fullId)
            val container: ContainerStats = new ContainerStats(containerId, ticksPassed)
            println("Recording " + fullId + " after " + (ticksPassed * intervalSeconds) + "s")
            containerCgroups.put(containerId, cgroup)
            containerStats += container
          }
          catch {
            case e: IOException => // skip this container, it is not on this node
          })
        }

        // collect data for available containers
        for (container <- containerStats) {
          val cgroup = containerCgroups(container.containerId)

          container.cpuUtil += tryOrElse(cgroup.getCurrentCpuUsage, Float.NaN)
          // TODO memory might not be managed using cgroups, fallback to other source
          container.memUtil += tryOrElse((cgroup.getCurrentMemUsage / 1024 / 1024).toInt, -1)

          onCollect(container)
        }
      }
    }

    executor.scheduleAtFixedRate(runnable, 0, intervalSeconds * 1000, TimeUnit.MILLISECONDS)
    this
  }

  /**
   * Stop recording and return all recorded container statistics.
   *
   * This also clears the recorded statistics.
   */
  def stopRecording(): Array[ContainerStats] = {
    executor.shutdownNow

    val results = containerStats.toArray
    containerStats.clear()
    containerCgroups.clear()

    results
  }

  def tryOrElse[T](f: => T, default: T): T = {
    try f
    catch {
      case e: Throwable =>
        println("AppStatsCollector encountered an error during stats collection for " + applicationId)
        e.printStackTrace()
        default
    }
  }

}
