package de.tuberlin.cit.freamon.cgroups

import java.io.File

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
    io.Source.fromFile(String.join("/", mountPath, controller, groupId, param)).mkString.trim
  }
}