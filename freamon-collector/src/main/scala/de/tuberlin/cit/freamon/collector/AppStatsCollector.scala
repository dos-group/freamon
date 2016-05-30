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
 * @param containerId ID of the monitored container: container_1455551433868_0002_01_123456
 * @param startTick ticks after application start when this container was started to be monitored
 */
case class ContainerStats(containerId: String, startTick: Long) {
  val blkioUtil = new ArrayBuffer[Float]
  val cpuUtil = new ArrayBuffer[Float]
  val netUtil = new ArrayBuffer[Float]
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
    val containerIds = args.drop(3)
    println("Starting for " + secondsToRun + "s with YARN config at " + yarnSitePath)
    println("Recording " + containerIds.length + " containers: " + containerIds.mkString(" "))

    val yarnConf = new YarnConfiguration()
    yarnConf.addResource(new FileInputStream(yarnSitePath), YARN_SITE_CONFIGURATION_FILE)

    val appStats = new AppStatsCollector(appId, yarnConf, 1)
    appStats.addContainers(containerIds)
    appStats.onCollect = container => {
      println(container.containerId + " BlkIO-avg: " + container.blkioUtil.last + " Bytes")
      println(container.containerId + " CPU-avg: " + container.cpuUtil.last + " cores")
      println(container.containerId + " Network-avg: " + container.netUtil.last + " Bytes")
      println(container.containerId + " Memory: " + container.memUtil.last + " MB")
    }

    appStats.startRecording()
    Thread.sleep(1000 * secondsToRun)
    val results = appStats.stopRecording()

    for (container <- results) {
      println(container.containerId + " BlkIO history: " + container.blkioUtil.mkString(", "))
      println(container.containerId + " CPU history: " + container.cpuUtil.mkString(", "))
      println(container.containerId + " Net history: " + container.netUtil.mkString(", "))
      println(container.containerId + " Memory history: " + container.memUtil.mkString(", "))
    }
  }
}

/**
 * Collects statistics from a single node, only for the specified containers.
 */
class AppStatsCollector(applicationId: String, yarnConfig: Configuration, intervalSeconds: Long) {

  private val executor = Executors.newScheduledThreadPool(1)

  val cgroupsMountPath = yarnConfig.get(NM_LINUX_CONTAINER_CGROUPS_MOUNT_PATH, "/sys/fs/cgroup/")
  val cgroupsHierarchy = yarnConfig.get(NM_LINUX_CONTAINER_CGROUPS_HIERARCHY, "/hadoop-yarn")

  val containerIds = new mutable.HashSet[String]()
  val containerCgroups = new mutable.HashMap[String, Cgroup]()
  val containerStats = new ArrayBuffer[ContainerStats]()
  var ticksPassed = 0

  var onCollect = (container: ContainerStats) => {}

  def addContainers(newContainerIds: Array[String]) = {
    containerIds ++= newContainerIds
    this
  }

  def startRecording() = {

    val runnable = new Runnable() {
      def run() {
        try {
          ticksPassed += 1

          // try to create previously unavailable container monitors
          for (containerId <- containerIds) {
            containerCgroups.getOrElse(containerId, {
              Cgroup.tryCreate(cgroupsMountPath, cgroupsHierarchy + "/" + containerId).map(cgroup => {
                val container = new ContainerStats(containerId, ticksPassed)
                println("Recording " + containerId + " after " + (ticksPassed * intervalSeconds) + "s")
                containerCgroups.put(containerId, cgroup)
                containerStats += container
              })
            })
          }

          // collect data for available containers
          for (container <- containerStats) {
            val cgroup = containerCgroups(container.containerId)

            container.blkioUtil += tryOrElse(cgroup.getAvgBlockIOUsage, -1)
            container.cpuUtil += tryOrElse(cgroup.getCurrentCpuUsage, -1)
            container.netUtil += tryOrElse(cgroup.getAvgNetworkUsage, -1)
            // TODO memory might not be managed using cgroups, fall back to other source
            container.memUtil += tryOrElse((cgroup.getCurrentMemUsage / 1024 / 1024).toInt, -1)

            onCollect(container)
          }
        }
        catch {
          case e: Throwable => e.printStackTrace()
        }
      }
    }

    executor.scheduleAtFixedRate(runnable, 0, intervalSeconds, TimeUnit.SECONDS)
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
      case e: IOException =>
        println(s"AppStatsCollector encountered an error during stats collection for $applicationId: $e")
        default
    }
  }

}
