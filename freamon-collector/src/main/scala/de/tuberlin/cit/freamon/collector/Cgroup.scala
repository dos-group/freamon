package de.tuberlin.cit.freamon.collector

import java.io.{File, FileInputStream, FileNotFoundException, IOException}
import java.util.concurrent.{Executors, TimeUnit}

import org.apache.hadoop.yarn.conf.YarnConfiguration
import org.apache.hadoop.yarn.conf.YarnConfiguration._

import scala.collection.mutable
import scala.io.Source
import scala.util.Try

import sys.process._


object Cgroup {
  val PARAM_BLKIO_THROTTLE_SERVICE = "blkio.throttle.io_service_bytes"
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
  val CONTROLLER_MEM = "memory"

  val DATA_DIR = "/data" // Directory used in blkio statistics
  val MOUNT_INFO = "/proc/self/mountinfo"

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

/**
 * Provides an interface to the raw data of one cgroup.
 */
class Cgroup {
  private final var mountPath: String = null
  private final var groupId: String = null
  private var lastBlockIOUsage: Long = 0
  private var lastBlockIOUsageTime: Long = 0
  private var lastCpuUsage: Long = 0
  private var lastCpuUsageTime: Long = 0
  private var lastNetUsage: Long = 0
  private var lastNetUsageTime: Long = 0

  private val blockDevice = getBlockDevice(getMountPoint(Cgroup.DATA_DIR), Cgroup.MOUNT_INFO)

  /**
   * Throws a FileNotFoundException if the cgroup does not exist in any implemented controller.
   * If only some cgroup params are unavailable, the instance will still be created,
   * so when they become available later, they can be read.
   */
  def this(mountPath: String, groupId: String) {
    this()
    this.mountPath = mountPath
    this.groupId = groupId

    val noBlockIO = Try {
      lastBlockIOUsage = getCurrentBlockIOUsage(blockDevice)
      lastBlockIOUsageTime = System.nanoTime
    }.isFailure

    val noCpu = Try {
      lastCpuUsage = readParam(Cgroup.CONTROLLER_CPU, Cgroup.PARAM_CPU_USAGE_TOTAL).toLong
      lastCpuUsageTime = System.nanoTime
    }.isFailure

    val noNet = Try {
      lastNetUsage = getCurrentNetworkUsage
      lastNetUsageTime = System.nanoTime
    }.isFailure

    if (noBlockIO && noCpu && noNet
      && !List(Cgroup.CONTROLLER_MEM, Cgroup.CONTROLLER_BLKIO, Cgroup.CONTROLLER_CPU, Cgroup.CONTROLLER_DEVICE)
        .exists(controller => new java.io.File(String.join("/", mountPath, controller, groupId))
          .exists)) {
      throw new FileNotFoundException("Could not open cgroup " + groupId)
    }
  }

  /** Retrieves mount point of given directory via stat. */
  def getMountPoint(dir: String): String = {
    ("stat -L -c %m " + dir !!).trim
  }

  /** Retrieves the block device of given mount point via mountinfo. */
  def getBlockDevice(mountPoint: String, mountInfo: String): String = {
    for (line <- Source.fromFile(mountInfo).getLines) {
      val parts = line.split(' ')

      if (parts(4) == mountPoint) {
        return parts(2)
      }
    }

    throw new IOException("Unable to find block device!")
  }

  /**
    * Parses and returns total usage in bytes from a io throttle service statistic.
    *
    * @param field: Read, Write, Sync, Async or Total (Default)
    */
  def parseBlockUsage(usage: String, blockDevice: String, field: String = "Total"): Long = {
    for (line <- usage.split('\n')) {
      val parts = line.split(' ')

      if (parts(0) == blockDevice && parts(1) == field) {
        return parts(2).toLong
      }
    }

    throw new IOException("Can't find block device in usage statistic.")
  }

  /** Retrieves the current block IO usage in bytes. */
  def getCurrentBlockIOUsage(blockDevice: String): Long = {
    parseBlockUsage(readParam(Cgroup.CONTROLLER_BLKIO, Cgroup.PARAM_BLKIO_THROTTLE_SERVICE), blockDevice)
  }

  /** Retrieves the average block IO usage since the last measurement in bytes. */
  def getAvgBlockIOUsage: Float = {
    val usageAbsolute = getCurrentBlockIOUsage(blockDevice)
    val now = System.nanoTime
    val timeDiff = (now - lastBlockIOUsageTime).toFloat * 1000 * 1000 * 1000 // in seconds
    val usage = (usageAbsolute - lastBlockIOUsage).toFloat / timeDiff
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

  /** Retrieves the current network usage in bytes. */
  def getCurrentNetworkUsage: Long = {
    0 // TODO
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
    try {
      val source = Source.fromFile(path)
      val paramVal = source.mkString.trim
      source.close()
      paramVal
    }
    catch {
      case e: IOException =>
        throw new IOException("Could not read cgroups parameter at " + path, e)
    }
  }
}
