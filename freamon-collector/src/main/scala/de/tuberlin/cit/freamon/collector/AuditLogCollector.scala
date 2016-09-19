package de.tuberlin.cit.freamon.collector

import java.io.{File, InputStream}
import java.text.SimpleDateFormat
import java.util

import de.tuberlin.cit.freamon.api.AuditLogEntry

object AuditLogCollector{
  var entries: util.ArrayList[AuditLogEntry] = new util.ArrayList[AuditLogEntry]()

  def getEntry: AuditLogEntry = {
    if (checkIfEmpty){
      entries.synchronized {
        entries.remove(0)
      }
    }
    else
      null
  }

  def checkIfEmpty: Boolean = {
    entries.synchronized{
      entries.isEmpty
    }
  }

  def getNumberOfEntries: Int = {
    entries.synchronized{
      entries.size()
    }
  }

  def getAllEntries: util.ArrayList[de.tuberlin.cit.freamon.api.AuditLogEntry] = {
    println("AuditLogCollector: getAllEntries called")
    println("There are "+ getNumberOfEntries +" entries.")
    if(!entries.isEmpty) {
      val result: util.ArrayList[AuditLogEntry] = new util.ArrayList[AuditLogEntry]()
      entries.synchronized {
      for (i <- 0 to entries.size()) {
        result.add(entries.remove(0))
      }
    }
      result
    }
    else
      null
  }

  def start(path: String) {
    def t() = new Thread(new Runnable {
      import java.io.{BufferedReader, InputStreamReader, File, InputStream}
      println("Extra thread started")
      val logFile = new File(path)
      val br = new BufferedReader(new InputStreamReader(follow(logFile)))
      println("Inside run...")
      println("The path is: "+path)
      println("logFile and br initialised")
      override def run(): Unit = {
        read

        def read: Unit = {
          println("Started executing read..")
          var l: String = null
          println("The value of l: "+l)
          try {
            l = br.readLine
          }
          catch {
            case e: Exception =>{
              println("Caught an exception: "+e.getCause)
              e.getStackTrace
            }
          }
          println("Contents of l: "+l)
          if(l != null){
            var e: AuditLogEntry = processEntry(l)
            println("Before synchronised block")
            println("Current object's date: "+e.date)
            entries.synchronized{
              entries.add(e)
              println("There are "+entries.size()+" entries now.")
            }
            println("After synchronised block")
            e = null
          }
          read
        }
        def processEntry(entry: String): AuditLogEntry = {

          val sdf: SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS")

          val date: String = entry.substring(0,23)
          val parsedDate = sdf.parse(date).getTime
          val report = entry.substring(49)
          val splitReport: Array[String] = report.split("\t")
          val allowed = splitReport(0).substring(8)
          var boolAllowed: Boolean = false
          if(allowed=="true")
            boolAllowed = true
          else if (allowed=="false")
            boolAllowed = false
          val ugi = splitReport(1).substring(4)
          val ip = splitReport(2).substring(3)
          val cmd = splitReport(3).substring(4)
          val src =splitReport(4).substring(4)
          val dst = splitReport(5).substring(4)
          val perm = splitReport(6).substring(5)
          val proto = splitReport(7).substring(6)

          val auditLog:AuditLogEntry = new AuditLogEntry(parsedDate, boolAllowed, ugi, ip, cmd, src, dst, perm, proto)
          auditLog

        }

        def follow(file: File): InputStream = {
          val maxRetries = 3
          val waitToOpen = 1000
          val waitBetweenReads = 100

          def sleep(msec: Long) = () => Thread.sleep(msec)

          follow2(file, maxRetries, sleep(waitToOpen), sleep(waitBetweenReads))
        }

        def follow2(file: File, openTries: Int, openSleep: () => Unit, rereadSleep: () => Unit): InputStream = {
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
    })
    t.start
  }
  /*def processFile(path: String): Unit = {
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
  }*/

  private def processEntry(entry: String): de.tuberlin.cit.freamon.api.AuditLogEntry = {
    val sdf: SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS")

    val date: String = entry.substring(0,23)
    val parsedDate = sdf.parse(date).getTime
    val report = entry.substring(49)
    val splitReport: Array[String] = report.split("\t")
    val allowed = splitReport(0).substring(8)
    var boolAllowed: Boolean = false
    if(allowed=="true")
      boolAllowed = true
    else if (allowed=="false")
      boolAllowed = false
    val ugi = splitReport(1).substring(4)
    val ip = splitReport(2).substring(3)
    val cmd = splitReport(3).substring(4)
    val src =splitReport(4).substring(4)
    val dst = splitReport(5).substring(4)
    val perm = splitReport(6).substring(5)
    val proto = splitReport(7).substring(6)

    val auditLog:AuditLogEntry = new AuditLogEntry(parsedDate, boolAllowed, ugi, ip, cmd, src, dst, perm, proto)
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
