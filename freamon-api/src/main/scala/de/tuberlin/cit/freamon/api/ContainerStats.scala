package de.tuberlin.cit.freamon.api

import scala.collection.mutable.ArrayBuffer

/**
  * Stores the recorded statistics about one container.
  *
  * @param containerId ID of the monitored container: container_1455551433868_0002_01_123456
  * @param startTick   ticks after application start when this container was started to be monitored
  */
case class ContainerStats(containerId: String, startTick: Long) {
  val blkioUtil = new ArrayBuffer[Float]
  val cpuUtil = new ArrayBuffer[Float]
  val netUtil = new ArrayBuffer[Float]
  val memUtil = new ArrayBuffer[Int]
}
