package de.tuberlin.cit.freamon.collector

import java.io.{File, IOException}
import java.util.concurrent.{Executors, TimeUnit}

import scala.collection.mutable
import scala.io.Source

/**
 * Provides an interface to the raw data of one cgroup.
 */
object Cgroup {
  val PARAM_CPU_USAGE_PER_CORE = "cpuacct.usage_percpu"
  val PARAM_CPU_USAGE_TOTAL = "cpuacct.usage"
  val PARAM_CPU_PERIOD = "cpu.cfs_period_us"
  val PARAM_CPU_QUOTA = "cpu.cfs_quota_us"
  val PARAM_MEM_USAGE = "memory.usage_in_bytes"
  val PARAM_MEM_LIMIT = "memory.limit_in_bytes"
  val CONTROLLER_CPU = "cpu,cpuacct"
  val CONTROLLER_MEM = "memory"

  def main(args: Array[String]) {
    if (args.length < 2) {
      println("Params: </path/to/yarn-site.xml> <seconds to run>")
      return
    }

    val yarnSitePath = args(0)
    val seconds = args(1).toInt
    println("Starting for " + seconds + "s with YARN config at " + yarnSitePath)

    val conf = new YarnConfig(yarnSitePath)
    val yarnCgroup = new Cgroup(conf.cgroupsMountPath, conf.cgroupsHierarchy)

    val cpuValues = new mutable.MutableList[Float]
    val memValues = new mutable.MutableList[Long]
    val subgroups = new mutable.HashSet[String]

    val runnable = new Runnable() {
      def run() {
        try {
          val currentCpuUsage = yarnCgroup.getCurrentCpuUsage
          val currentMemUsage = yarnCgroup.getCurrentMemUsage
          val currentSubgroups = yarnCgroup.getSubgroups
          println("CPU: " + currentCpuUsage)
          println("Memory: " + currentMemUsage)
          println("Subgroups: " + currentSubgroups.toString)
          cpuValues += currentCpuUsage
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

    println("CPU history: " + cpuValues)
    println("Memory history: " + memValues)
    println("All subgroups: " + subgroups)
  }
}

class Cgroup {
  private final var mountPath: String = null
  private final var groupId: String = null
  private var lastCpuUsage: Long = 0
  private var lastCpuUsageTime: Long = 0

  def this(mountPath: String, groupId: String) {
    this()
    this.mountPath = mountPath
    this.groupId = groupId
    lastCpuUsage = readParam(Cgroup.CONTROLLER_CPU, Cgroup.PARAM_CPU_USAGE_TOTAL).toLong
    lastCpuUsageTime = System.nanoTime
  }

  /**
   * Retrieves the average CPU usage since the last measurement relative to one core.
   *
   * @return usage; relative to one core, thus can be > 1.0
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
   * Retrieves how many bytes of memory are used by this group.
   *
   * @return share in bytes, or -1 if unlimited
   */
  def getCurrentMemUsage = {
    readParam(Cgroup.CONTROLLER_MEM, Cgroup.PARAM_MEM_USAGE).toLong
  }

  /**
   * Retrieves how many bytes of memory can be used by this group.
   *
   * @return share in bytes, or -1 if unlimited
   */
  def getMemLimit = {
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
