package de.tuberlin.cit.freamon.collector

import java.io.{FileNotFoundException, File, FileInputStream, IOException}
import java.util.concurrent.{Executors, TimeUnit}

import org.apache.hadoop.yarn.conf.YarnConfiguration
import org.apache.hadoop.yarn.conf.YarnConfiguration._

import scala.collection.mutable
import scala.io.Source

/**
 * Provides an interface to the raw data of one cgroup.
 */
object Cgroup {
  val PARAM_BLKIO_SECTORS = "blkio.sectors"
  val PARAM_CPU_USAGE_PER_CORE = "cpuacct.usage_percpu"
  val PARAM_CPU_USAGE_TOTAL = "cpuacct.usage"
  val PARAM_CPU_PERIOD = "cpu.cfs_period_us"
  val PARAM_CPU_QUOTA = "cpu.cfs_quota_us"
  val PARAM_TASKS = "tasks"
  val PARAM_MEM_USAGE = "memory.usage_in_bytes"
  val PARAM_MEM_LIMIT = "memory.limit_in_bytes"
  val CONTROLLER_BLKIO = "blkio"
  val CONTROLLER_CPU = "cpu,cpuacct"
  val CONTROLLER_DEVICE = "devices"
  val CONTROLLER_NET = "net_cls,net_prio"
  val CONTROLLER_MEM = "memory"
  val NET_DEV_FILE_PATTERN = "/proc/%d/net/dev"

  def main(args: Array[String]) {
    if (args.length < 2) {
      println("Params: </path/to/yarn-site.xml> <seconds to run>")
      return
    }

    val yarnSitePath = args(0)
    val seconds = args(1).toInt
    println("Starting for " + seconds + "s with YARN config at " + yarnSitePath)

    val conf = new YarnConfiguration()
    conf.addResource(new FileInputStream(yarnSitePath), YARN_SITE_CONFIGURATION_FILE)
    val yarnCgroup = new Cgroup(
      conf.get(NM_LINUX_CONTAINER_CGROUPS_MOUNT_PATH, "/sys/fs/cgroup/"),
      conf.get(NM_LINUX_CONTAINER_CGROUPS_HIERARCHY, "/hadoop-yarn"))

    val blkioValues = new mutable.MutableList[Float]
    val cpuValues = new mutable.MutableList[Float]
    val netValues = new mutable.MutableList[Float]
    val memValues = new mutable.MutableList[Long]
    val subgroups = new mutable.HashSet[String]

    val runnable = new Runnable() {
      def run() {
        try {
          val currentBlkIOUsage = yarnCgroup.getAvgBlockIOUsage
          val currentCpuUsage = yarnCgroup.getCurrentCpuUsage
          val currentNetUsage = yarnCgroup.getAvgNetworkUsage
          val currentMemUsage = yarnCgroup.getCurrentMemUsage
          val currentSubgroups = yarnCgroup.getSubgroups
          println("BlkIO: " + currentBlkIOUsage)
          println("CPU: " + currentCpuUsage)
          println("Network: " + currentNetUsage)
          println("Memory: " + currentMemUsage)
          println("Subgroups: " + currentSubgroups.toString)
          blkioValues += currentBlkIOUsage
          cpuValues += currentCpuUsage
          netValues += currentNetUsage
          memValues += currentMemUsage
          subgroups.union(currentSubgroups)
        }
        catch {
          case e: IOException =>
            e.printStackTrace()
        }
      }
    }

    val executor = Executors.newScheduledThreadPool(1)
    executor.scheduleAtFixedRate(runnable, 0, 250, TimeUnit.MILLISECONDS)
    Thread.sleep(1000 * seconds)
    executor.shutdownNow

    println("BlkIO history: " + blkioValues)
    println("CPU history: " + cpuValues)
    println("Net history: " + netValues)
    println("Memory history: " + memValues)
    println("All subgroups: " + subgroups)
  }
}

class Cgroup {
  private final var mountPath: String = null
  private final var groupId: String = null
  private var lastBlockIOUsage: Long = 0
  private var lastBlockIOUsageTime: Long = 0
  private var lastCpuUsage: Long = 0
  private var lastCpuUsageTime: Long = 0
  private var lastNetUsage: Long = 0
  private var lastNetUsageTime: Long = 0

  def this(mountPath: String, groupId: String) {
    this()
    this.mountPath = mountPath
    this.groupId = groupId
    lastBlockIOUsage = getCurrentBlockIOUsage
    lastBlockIOUsageTime = System.nanoTime
    lastCpuUsage = readParam(Cgroup.CONTROLLER_CPU, Cgroup.PARAM_CPU_USAGE_TOTAL).toLong
    lastCpuUsageTime = System.nanoTime
    lastNetUsage = getCurrentNetworkUsage
    lastNetUsageTime = System.nanoTime
  }

  /** Parses and sums up all sector from a sector statistic. */
  def parseBlockUsage(usage: String): Long = {
    usage.split('\n').map(_.split(' ').last.toLong).sum
  }

  /** Retrieves the current block IO usage in sectors. */
  def getCurrentBlockIOUsage: Long = {
    parseBlockUsage(readParam(Cgroup.CONTROLLER_BLKIO, Cgroup.PARAM_BLKIO_SECTORS))
  }

  /** Retrieves the average block IO usage since the last measurement in sectors. */
  def getAvgBlockIOUsage: Float = {
    val usageAbsolute = getCurrentBlockIOUsage
    val now = System.nanoTime
    val usage = (usageAbsolute - lastBlockIOUsage).toFloat / (now - lastBlockIOUsageTime).toFloat
    lastBlockIOUsage = usageAbsolute
    lastBlockIOUsageTime = now
    usage
  }

  /**
   * Retrieves the average CPU usage since the last measurement relative to one core.
   *
   * @return usage in cores, can be > 1.0
   */
  def getCurrentCpuUsage: Float = {
    val usageAbsolute = readParam(Cgroup.CONTROLLER_CPU, Cgroup.PARAM_CPU_USAGE_TOTAL).toLong
    val now = System.nanoTime
    val usage = (usageAbsolute - lastCpuUsage).toFloat / (now - lastCpuUsageTime).toFloat
    lastCpuUsage = usageAbsolute
    lastCpuUsageTime = now
    usage
  }

  /**
   * Retrieves how many cores can be used by this group.
   * Calculated as quota per period.
   *
   * @return share in number of (partial) cores, or Float.PositiveInfinity if unlimited (quota == -1)
   */
  def getCpuShare: Float = {
    val quota = readParam(Cgroup.CONTROLLER_CPU, Cgroup.PARAM_CPU_QUOTA).toLong
    if (quota == -1) {
      return Float.PositiveInfinity
    }
    val period = readParam(Cgroup.CONTROLLER_CPU, Cgroup.PARAM_CPU_PERIOD).toLong
    quota.toFloat / period.toFloat
  }

  /**
    * Parse a task specific network usage statistic (e.g. from /proc/.../net/dev).
    *
    * @return rx and tx usage in bytes
    */
  def parseNetworkUsage(inputFile: String): Long = {
    try {
      var sum = 0l
      for (line <- Source.fromFile(inputFile).getLines.drop(2)) {
        val values = line.replace("^ +", "").split(" +")
        sum += values(2).toLong  // RX
        sum += values(10).toLong // TX
      }
      sum
    } catch {
      case e: FileNotFoundException => 0l // task already finished
    }
  }

  /** Retrieves the current network usage in bytes of a given task. */
  def getCurrentTaskNetworkUsage(task: Long): Long = {
    parseNetworkUsage(Cgroup.NET_DEV_FILE_PATTERN.format(task))
  }

  /** Retrieves the current network usage in bytes. */
  def getCurrentNetworkUsage: Long = {
    readParam(Cgroup.CONTROLLER_DEVICE, Cgroup.PARAM_TASKS)
      .split("\n")
      .map((task) => getCurrentTaskNetworkUsage(task.toLong))
      .sum
  }

  /** Retrieves the average network usage since the last measurement in bytes. */
  def getAvgNetworkUsage: Float = {
    val usageAbsolute = getCurrentNetworkUsage
    val now = System.nanoTime
    val usage = (usageAbsolute - lastNetUsage).toFloat / (now - lastNetUsageTime).toFloat
    lastNetUsage = usageAbsolute
    lastNetUsageTime = now
    usage
  }

  /**
   * Retrieves how many bytes of memory are used by this group.
   *
   * @return usage in bytes
   */
  def getCurrentMemUsage: Long = {
    readParam(Cgroup.CONTROLLER_MEM, Cgroup.PARAM_MEM_USAGE).toLong
  }

  /**
   * Retrieves how many bytes of memory can be used by this group.
   *
   * @return share in bytes, or -1 if unlimited
   */
  def getMemLimit: Long = {
    readParam(Cgroup.CONTROLLER_MEM, Cgroup.PARAM_MEM_LIMIT).toLong
  }

  def getSubgroups = {
    val cpuDirs = new File(String.join("/", mountPath, Cgroup.CONTROLLER_CPU, groupId)).listFiles.filter(_.isDirectory).map(_.getName)
    val memDirs = new File(String.join("/", mountPath, Cgroup.CONTROLLER_MEM, groupId)).listFiles.filter(_.isDirectory).map(_.getName)
    (cpuDirs ++ memDirs).toSet
  }

  private def readParam(controller: String, param: String): String = {
    val path = String.join("/", mountPath, controller, groupId, param)
    val source = Source.fromFile(path)
    try source.mkString.trim
    catch {
      case e: IOException =>
        throw new IOException("Could not read cgroups parameter at " + path, e)
    }
    finally source.close()
  }
}
