package de.tuberlin.cit.freamon.collector

import java.io.IOException

import scala.io.Source

class IoPoll(pid: String) {
  var prevProcSample: Map[String, Double] = IoPoll.getSample(pid)
  var prevProcSampleTime: Long = System.currentTimeMillis()

  /**
    * Sample /proc/(pid)/io and calculate the bytes per second since the last call
    *
    * uses {read,write}_bytes to get "the number of bytes which this process
    * really did cause to be fetched from the storage layer"
    * opposed to [rw]char which yields "the sum of bytes which this process
    * passed to read()/write() and pread()/pwrite()"
    * See also https://git.kernel.org/cgit/linux/kernel/git/torvalds/linux.git/tree/Documentation/filesystems/proc.txt?id=HEAD#n1570
    *
    * @return 2-tuple of read and written bytes per second since the last sample
    */
  def getCurrentStorageIoRate(): (Double, Double) = {
    val sampleTime = System.currentTimeMillis()
    val sample = IoPoll.getSample(pid)

    val dt = sampleTime - prevProcSampleTime
    val deltaRead = sample("read_bytes") - prevProcSample("read_bytes")
    val deltaWrite = sample("write_bytes") - prevProcSample("write_bytes")

    prevProcSampleTime = sampleTime
    prevProcSample = sample

    (deltaRead * 1000d / dt, deltaWrite * 1000d / dt)
  }
}

object IoPoll {
  /**
    * Simply reads /proc/(pid)/io and converts it into a more usable map format
    *
    * @param pid process id of which to get the storage layer io info
    * @return map of string => double containing the key/value pairs in the io file
    */
  def getSample(pid: String): Map[String, Double] = {
    val path = "/proc/" + pid + "/io"
    val fileContent = try {
      val source = Source.fromFile(path)
      val fileContent = source.mkString.trim
      source.close()
      fileContent
    }
    catch {
      case e: IOException =>
        throw new IOException("Could not read io stats at " + path, e)
    }
    fileContent.split("\n").map(_.split(": ")).map {
      case Array(k: String, v: String) => (k, java.lang.Double.parseDouble(v))
    }.toMap
  }
}
