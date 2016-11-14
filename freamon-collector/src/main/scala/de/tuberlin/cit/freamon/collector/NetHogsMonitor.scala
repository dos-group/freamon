package de.tuberlin.cit.freamon.collector

import scala.sys.process.{Process, ProcessLogger}

case class NetUsageSample(time: Long, pid: Long, transmit: Double, receive: Double)

object NetHogsMonitor {
  def main(args: Array[String]): Unit = {
    new NetHogsMonitor("nethogs", r =>
      printf("proc %s tx %s rx %s at %s%n", r.pid, r.transmit, r.receive, r.time)
    )
  }

  /**
    * @param line NetHogs stdout potentially containing process network usage data
    * @return pid and timestamped up and down usage in KB/s, or None if this line did not contain any usage info
    */
  def processLine(line: String): Option[NetUsageSample] = {
    if (!line.contains("\t"))
      return None
    try {
      val now = System.currentTimeMillis()
      val Array(procStr, upStr, downStr) = line.split("\t")
      val procSplit = procStr.split("/")
      val pid = java.lang.Long.parseLong(procSplit(procSplit.length - 2))
      val tx = java.lang.Double.parseDouble(upStr)
      val rx = java.lang.Double.parseDouble(downStr)
      Some(NetUsageSample(now, pid, tx, rx))
    } catch {
      case e: Throwable =>
        println(s"NetHogs failed processing line '$line': $e")
        None
    }
  }

}

/** Runs the NetHogs command, parses its output, and calls the callback.
  */
class NetHogsMonitor(netHogsCommand: String, sendSample: NetUsageSample => Any) {
  val process = Process(netHogsCommand + " -t -v 0").run(new NetHogsTraceParser(sendSample))
}

class NetHogsTraceParser(sendSample: NetUsageSample => Any) extends ProcessLogger() {

  override def out(s: => String) = NetHogsMonitor.processLine(s).map(sendSample)

  override def err(s: => String): Unit = println("[NetHogs StdErr] " + s)

  override def buffer[T](f: => T): T = f
}
