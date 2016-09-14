package de.tuberlin.cit.freamon.collector

import java.io.{BufferedReader, File, InputStream, InputStreamReader}
import java.text.SimpleDateFormat
import java.util

object AuditLogCollector{
  var entries: util.ArrayList[de.tuberlin.cit.freamon.api.AuditLogEntry] = new util.ArrayList[de.tuberlin.cit.freamon.api.AuditLogEntry]()
  def getEntry: de.tuberlin.cit.freamon.api.AuditLogEntry = {
    if (!entries.isEmpty)
      entries.remove(0)
    else
      null
  }

  def getAllEntries: util.ArrayList[de.tuberlin.cit.freamon.api.AuditLogEntry] = {
    if(!entries.isEmpty){
      val result: util.ArrayList[de.tuberlin.cit.freamon.api.AuditLogEntry] = new util.ArrayList[de.tuberlin.cit.freamon.api.AuditLogEntry]()
      for(i <- 0 to entries.size()){
        result.add(entries.remove(0))
      }
      result
    }
    else
      null
  }

  def anyEntryStored: Boolean = !entries.isEmpty

  def start(path: String) {
    def t() = new Thread(new Runnable {
      override def run(): Unit = {
        val logFile = new File(path)
        val br = new BufferedReader(new InputStreamReader(follow(logFile)))

        def read(): Unit = {
          val l = br.readLine()
          if(l != null)
            entries.add(processEntry(l))
          read()
        }
        read()
      }
    })
    t.start
  }
  def processFile(path: String): Unit = {
    def t = new Thread(new Runnable {
      override def run(): Unit = {
        val logFile = new File(path)
        val br = new BufferedReader(new InputStreamReader(follow(logFile)))

        def read(): Unit = {
          val l = br.readLine()
          if(l != null)
            entries.add(processEntry(l))
          read()
        }
        read()
      }
    })
    t.start
  }

  private def processEntry(entry: String): de.tuberlin.cit.freamon.api.AuditLogEntry = {
    val auditLog:de.tuberlin.cit.freamon.api.AuditLogEntry = new de.tuberlin.cit.freamon.api.AuditLogEntry
    val sdf: SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS")

    val date: String = entry.substring(0,23)
    auditLog.date = sdf.parse(date).getTime
    val report = entry.substring(49)
    val splitReport: Array[String] = report.split("\t")
    val allowed = splitReport(0).substring(8)
    if(allowed=="true")
      auditLog.allowed = true
    else if (allowed=="false")
      auditLog.allowed = false
    auditLog.ugi = splitReport(1).substring(4)
    auditLog.ip = splitReport(2).substring(3)
    auditLog.cmd = splitReport(3).substring(4)
    auditLog.src =splitReport(4).substring(4)
    auditLog.dst = splitReport(5).substring(4)
    auditLog.perm = splitReport(6).substring(5)
    auditLog.proto = splitReport(7).substring(6)

    auditLog

  }

  private def follow(file: File): InputStream = {
    val maxRetries = 3
    val waitToOpen = 1000
    val waitBetweenReads = 100

    def sleep(msec: Long) = () => Thread.sleep(msec)

    follow(file, maxRetries, sleep(waitToOpen), sleep(waitBetweenReads))
  }

  private def follow(file: File, openTries: Int, openSleep: () => Unit, rereadSleep: () => Unit): InputStream = {
    import java.io.SequenceInputStream

    val e = new util.Enumeration[InputStream]() {
      def nextElement = new FollowingInputStream(file, rereadSleep)
      def hasMoreElements = testExists(file, openTries, openSleep)
    }
    new SequenceInputStream(e)

  }
  def testExists(file: File, tries: Int, sleep: () => Unit): Boolean = {
    def tryExists(n: Int): Boolean =
      if(file.exists()) true
      else if (n > tries) false
      else {
        sleep()
        tryExists(n+1)
      }
    tryExists(1)
  }

}

import java.io.{File, InputStream}

class FollowingInputStream(val file:File, val waitForNewInput: () => Unit) extends InputStream{
  import java.io.FileInputStream
  private val underlying = new FileInputStream(file)

  def read: Int = handle(underlying.read())

  override def read(b: Array[Byte]): Int = read(b, 0, b.length)

  override def read(b: Array[Byte], off: Int, len: Int): Int =
    handle(underlying.read(b, off, len))

  override def close(): Unit = underlying.close()

  protected def rotated_? = try { underlying.getChannel.position() > file.length() }

  protected def closed_? = !underlying.getChannel.isOpen

  protected def handle(read: => Int): Int = read match {
    case -1 if rotated_? || closed_? => -1
    case -1 =>
      waitForNewInput()
      handle(read)
    case i => 1
  }
  require(file != null)
  assume(file.exists())
}
