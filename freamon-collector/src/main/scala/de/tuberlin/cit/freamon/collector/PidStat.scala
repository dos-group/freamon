package de.tuberlin.cit.freamon.collector

import scala.collection.mutable
import scala.sys.process.{Process, ProcessLogger}

/**
  * Sample from pidstat command, run as "(pidstat) -duh 1"
  * @param time seconds since UNIX epoch
  * @param pid process id of the monitored process
  * @param usr CPU percentage at application level
  * @param sys CPU percentage at kernel level
  * @param read number of kilobytes read from disk per second
  * @param write number of kilobytes written to disk per second
  */
case class PidStatSample(time: Long, pid: Long, usr: Double, sys: Double, read: Double, write: Double)

object PidStatMonitor {
  def main(args: Array[String]): Unit = {
    new PidStatMonitor("pidstat -C chromium", r =>
      printf("pidstat: pid=%s usr=%s sys=%s rd=%s wr=%s at %s%n", r.pid, r.usr, r.sys, r.read, r.write, r.time)
    )
  }
}

/** Runs the PidStat command, parses its output, and calls the callback.
  */
class PidStatMonitor(pidStatCommand: String, sendSample: PidStatSample => Any) {
  val process = Process(pidStatCommand + " -duh 1").run(new PidStatTraceParser(processLine))

  val format = new mutable.HashMap[String, Int]()

  /**
    * @param line PidStat stdout potentially containing process resource usage data
    * @return pid and timestamped up and down usage in KB/s, or None if this line did not contain any usage info
    */
  def processLine(line: String) {
    if (line.isEmpty || line.endsWith(" CPU)")) // empty or system info
      return
    if (line.startsWith("# ")) {
      // format specifier
      // example: #      Time   UID       PID    %usr %system  %guest    %CPU   CPU   kB_rd/s   kB_wr/s kB_ccwr/s iodelay  Command
      for ((col, i) <- line.split(" +").tail.zipWithIndex) {
        format.put(col, i)
      }
      return
    }
    try {
      val fields = line.stripPrefix(" ").split(" +")
      val now = java.lang.Integer.parseInt(fields(0)) * 1000
      val pid = java.lang.Long.parseLong(fields(format("PID")))
      val usr = java.lang.Double.parseDouble(fields(format("%usr")))
      val sys = java.lang.Double.parseDouble(fields(format("%system")))
      val read = java.lang.Double.parseDouble(fields(format("kB_rd/s")))
      val write = java.lang.Double.parseDouble(fields(format("kB_wr/s")))
      sendSample(PidStatSample(now, pid, usr, sys, read, write))
    } catch {
      case e: Throwable =>
        println(s"PidStat failed processing line '$line': $e")
    }
  }
}

class PidStatTraceParser(processLine: String => Any) extends ProcessLogger() {

  override def out(s: => String) = processLine(s)

  override def err(s: => String): Unit = println("[PidStat StdErr] " + s)

  override def buffer[T](f: => T): T = f
}
