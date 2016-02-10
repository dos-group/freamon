package de.tuberlin.cit.freamon.cgroups

import java.io.IOException
import java.util.concurrent.{Executors, TimeUnit}

import scala.collection.mutable

object Main {
  def main(args: Array[String]) {
    if (args.length < 2) {
      println("Usage: java -jar cgstats.jar </path/to/yarn-site.xml> <seconds to run>")
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
