package de.tuberlin.cit.freamon.collector

import org.hyperic.sigar.Sigar
import org.hyperic.sigar.ptql.ProcessFinder

object Proc {
  val sigar = new Sigar()

  def getForContainer(container: String): Option[Proc] = {
    // maybe additionally filter for org.apache.flink.yarn.YarnTaskManagerRunner
    val searchPattern = s"State.Name.eq=java,Args.*.ct=$container"
    val pids: Array[Long] = ProcessFinder.find(Proc.sigar, searchPattern)
    if (pids.length < 1) return None
    if (pids.length > 1)
      throw new IllegalStateException(s"Search pattern is incomplete, found too many (${pids.length}) matching processes")

    Some(new Proc(pids(0)))
  }
}

class Proc(pid: Long) {
  def getCurrentCpuUsage: Double = {
    Proc.sigar.getProcCpu(pid).getPercent
  }

  def getCurrentMemUsage: Long = {
    Proc.sigar.getProcMem(pid).getSize
  }

  //  getCurrentBlockIOUsage
  //  getAvgBlockIOUsage
  //  getCurrentNetworkUsage
  //  getAvgNetworkUsage
}
