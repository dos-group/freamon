package de.tuberlin.cit.freamon.collector

import scala.sys.process.{Process, ProcessLogger}

/**
  * Data object to hold the gathered statistics
  * @param time - UNIX time
  * @param cpuUsr - user
  * @param cpuSys - system
  * @param cpuIdl - idle
  * @param cpuWai - wait
  * @param diskRd - read
  * @param diskWr - write
  * @param netRx - receive
  * @param netTx - send
  * @param mem - used
  */
case class DstatSample(time: Long, cpuUsr: Double, cpuSys: Double, cpuIdl: Double, cpuWai: Double,
                       diskRd: Double, diskWr: Double, netRx: Double, netTx: Double, mem: Double)

object DstatMonitor{
  def main(args: Array[String]): Unit = {
    new DstatMonitor(r =>
    printf(
      s"""time: %s, cpuUsr: %f, cpuSys: %f, cpuIdl: %f, cpuWai: %f, diskRd: %f, diskWr: %f, netRx: %f, netTx: %f, mem: %f%n""".stripMargin,
      r.time, r.cpuUsr, r.cpuSys, r.cpuIdl, r.cpuWai, r.diskRd, r.diskWr, r.netRx, r.netTx, r.mem))
  }

  /**
    * Function for parsing dstat output
    * @param line - line received from dstat
    * @return - a DstatSample object
    */
  def processLine(line: String): Option[DstatSample] = {
    if (line.contains("total"))
      return None
    if (line.contains("usr") || line.contains("read"))
      return None
    try {
      val now = System.currentTimeMillis()
      val pipeFreeString = line.replace('|','#')
      val spaceFreeString = pipeFreeString.replaceAll(" ", "#")
      val Array(usr, sys, idl, wai, _, _, dRead, dWrit, nRx, nTx, mUsed, _, _, _) = cleanString(spaceFreeString)
      val cpuUsr = cleanNumber(usr)
      val cpuSys = cleanNumber(sys)
      val cpuIdl = cleanNumber(idl)
      val cpuWai = cleanNumber(wai)
      val dskRead = cleanNumber(dRead)
      val dskWrit = cleanNumber(dWrit)
      val netRx = cleanNumber(nRx)
      val netTx = cleanNumber(nTx)
      val memUsed = cleanNumber(mUsed)
      Some(DstatSample(now, cpuUsr, cpuSys, cpuIdl, cpuWai, dskRead, dskWrit, netRx, netTx, memUsed))
    }
    catch {
      case e: Throwable =>
        println(s"Line processing from dstat failed on line: '$line': $e")
        e.printStackTrace()
        None
    }
  }

  /**
    * Function for conversion of dstat numbers containing letters (conversion based on SI units ie. k = 1000 and not k = 1024)
    * @param number - number to be converted
    * @return - cleaned number
    */
  def cleanNumber(number: String): Double = {
    var result: Double = -1
    if (number.contains("B"))
      java.lang.Double.parseDouble(number.replace("B",""))
    else if(number.contains("k")){
      val no = number.replace("k","")
      val realNumber = java.lang.Double.parseDouble(no)
      result = realNumber * 1000
    }
    else if (number.contains("M")){
      val no = number.replace("M", "")
      val realNumber = java.lang.Double.parseDouble(no)
      result = realNumber * 1000 * 1000
    }
    else if (number.contains("G")){
      val no = number.replace("G", "")
      val realNumber = java.lang.Double.parseDouble(no)
      result = realNumber * 1000 * 1000 * 1000
    }
    else if (number.contains("T")){
      val no = number.replace("T", "")
      val realNumber = java.lang.Double.parseDouble(no)
      result = realNumber * 1000 * 1000 * 1000 * 1000
    }
    else if (number.contains("P")){
      val no = number.replace("P", "")
      val realNumber = java.lang.Double.parseDouble(no)
      result = realNumber * 1000 * 1000 * 1000 * 1000 * 1000
    }
    else if (number.contains("E")){
      val no = number.replace("E", "")
      val realNumber = java.lang.Double.parseDouble(no)
      result = realNumber * 1000 * 1000 * 1000 * 1000 * 1000 * 1000
    }
    else if (number.contains("Z")){
      val no = number.replace("Z", "")
      val realNumber = java.lang.Double.parseDouble(no)
      result = realNumber * 1000 * 1000 * 1000 * 1000 * 1000 * 1000 * 1000
    }
    else if (number.contains("Y")){
      val no = number.replace("Y", "")
      val realNumber = java.lang.Double.parseDouble(no)
      result = realNumber * 1000 * 1000 * 1000 * 1000 * 1000 * 1000 * 1000 * 1000
    }
    else if (!number.contains("B") && !number.contains("k") && !number.contains("M") && !number.contains("G")
      && !number.contains("T") && !number.contains("P") && !number.contains("E") && !number.contains("Z") && !number.contains("Y"))
      result = java.lang.Double.parseDouble(number)

    else
      result = -1

    result
  }

  /**
    * Function for replacing '#' characters if they exist more than once at a given position. If there is a hash parameter at the
    * front of the string, it will be removed.
    * @param line - string to be cleaned from excessive hashes
    * @return - string cleaned from excessive hashes
    */
  def cleanString(line: String): Array[String] = {
    var result = ""
    if (line.contains("##")){
      result = line
      while (result.contains("##"))
        result = result.replace("##","#")
    }
    result = result.substring(1)
    val resultArray = result.split('#')
    resultArray
  }
}

/**
  * Class to run the dstat process
  * @param sendSample - carrier for the received line from dstat
  */
class DstatMonitor(sendSample: DstatSample => Any) {
  val process = Process("dstat -c -C total -d -D total -n -N total -m --noheaders").run(new DstatTraceParser(sendSample))
}

/**
  * Class allowing the diversion of the dstat output into processLine function.
  * @param sendSample - dstat line to be diverted
  */
class DstatTraceParser(sendSample: DstatSample => Any) extends ProcessLogger() {

  override def out(s: => String) = DstatMonitor.processLine(s).map(sendSample)

  override def err(s: => String): Unit = println("[Dstat StdErr] " + s)

  override def buffer[T](f: => T): T = f
}
