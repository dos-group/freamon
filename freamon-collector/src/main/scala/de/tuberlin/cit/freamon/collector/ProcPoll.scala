package de.tuberlin.cit.freamon.collector

import java.io.{FileInputStream, IOException}
import java.util.concurrent.{Executors, TimeUnit}

import de.tuberlin.cit.freamon.api.StatSample
import org.apache.hadoop.yarn.conf.YarnConfiguration
import org.apache.hadoop.yarn.conf.YarnConfiguration._
import org.hyperic.sigar.ptql.ProcessFinder
import org.hyperic.sigar.{Sigar, SigarException}

import scala.collection.mutable

object ProcPoll {

  val sigar = new Sigar()

  def main(args: Array[String]) {
    if (args.length < 2) {
      println("Params: </path/to/yarn-site.xml> <seconds to run> [containerID ...]")
      return
    }

    val yarnSitePath = args(0)
    val secondsToRun = args(1).toInt
    val containerIds = args.drop(2)
    println("Starting for " + secondsToRun + "s with YARN config at " + yarnSitePath)
    println("Recording " + containerIds.length + " containers: " + containerIds.mkString(" "))

    val yarnConf = new YarnConfiguration()
    yarnConf.addResource(new FileInputStream(yarnSitePath), YARN_SITE_CONFIGURATION_FILE)

    val appStats = new ProcPoll(1, { case StatSample(containerId, kind, millis, value) =>
      println(s"$millis $containerId $kind $value")
    })

    appStats.addContainers(containerIds)
    appStats.startRecording()

    Thread.sleep(1000 * secondsToRun)
  }

  def findContainerPid(container: String): Option[Long] = {
    val searchPattern = s"State.Name.eq=java,Args.*.ct=$container"
    val pids: Array[Long] = ProcessFinder.find(sigar, searchPattern)
    if (pids.length < 1) return None
    if (pids.length > 1)
      throw new IllegalStateException(s"Search pattern is incomplete, found too many (${pids.length}) matching processes")

    Some(pids(0))
  }
}

/**
  * Polls /proc every second using Sigar, emits events for collected usage data.
  */
class ProcPoll(intervalSeconds: Long, sendSample: StatSample => Any) {
  private val executor = Executors.newScheduledThreadPool(1)

  /** what containers will be polled, and their pid if they have started yet
    */
  val containerPids = new mutable.HashMap[String, Option[Long]]()

  /** reverse map of containerPids for running containers (known pid)
    */
  val containersByPid = new mutable.HashMap[Long, String]()

  def addContainers(newContainerIds: Array[String]) = {
    newContainerIds.filterNot(containerPids.contains)
      .map(containerPids.put(_, None))
    this
  }

  def startRecording() = {

    val runnable = new Runnable() {
      def run() {
        try {
          // find pids of previously unavailable containers
          for ((containerId, None) <- containerPids.filter(_._2.isEmpty)) {
            ProcPoll.findContainerPid(containerId) foreach { pid =>
              containerPids.put(containerId, Some(pid))
              containersByPid.put(pid, containerId)
              println(s"Started recording $containerId at pid $pid")
            }
          }

          // collect data for available containers
          // clone filtered array so we can remove stopped containers
          for ((containerId, Some(pid)) <- containerPids.filter(_._2.isDefined).clone()) {
            tryGetAndSend(containerId, 'cpu,
              ProcPoll.sigar.getProcCpu(pid).getPercent)

            tryGetAndSend(containerId, 'mem,
              ProcPoll.sigar.getProcMem(pid).getSize / 1024d / 1024d)
          }
        }
        catch {
          // keep the loop alive
          case e: Throwable => e.printStackTrace()
        }
      }
    }

    executor.scheduleAtFixedRate(runnable, 0, intervalSeconds, TimeUnit.SECONDS)
    this
  }

  def tryGetAndSend(containerId: String, kind: Symbol, valueFn: => Double) {
    try {
      sendSample(StatSample(containerId, kind, System.currentTimeMillis(), valueFn))
      return
    }
    catch {
      case e: IOException =>
        println(s"ProcPoll failed at cgroups while getting $kind for $containerId: $e")
      case e: SigarException =>
        println(s"ProcPoll failed at proc while getting $kind for $containerId: $e")
    }

    // container might have stopped, remove pid so it can get re-fetched next iteration
    containerPids.remove(containerId) match {
      case Some(Some(oldPid)) =>
        containersByPid.remove(oldPid)
          .foreach(containerPids.put(_, None))
      case _ =>
    }
  }

}
