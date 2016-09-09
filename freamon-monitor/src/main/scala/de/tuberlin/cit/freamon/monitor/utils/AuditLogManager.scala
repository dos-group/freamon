package de.tuberlin.cit.freamon.monitor.utils

import java.io.{File, InputStream}
import java.sql.{Connection, DriverManager}

import de.tuberlin.cit.freamon.results.AuditLogModel

/**
  * Object handling the communication between the log reader and database writer
  */
object AuditLogManager {

  var pathToLog: String = ""

  def receiveRequest(args: Array[String]): Unit = {
    if ((args.length==4) && args(2)=="--hdfs-audit"){
      pathToLog = args(3)
      loadDriver("nl.cwi.monetdb.jdbc.MonetDriver")
      createTable()
      start(pathToLog)
    }
  }

  private def processEntry(entry: String): Unit = {
    //var alm: AuditLogModel = null

    val date: String = entry.substring(0,23)
    println("Date: "+date)
    val status = entry.substring(24,27)
    println("Status: "+status)
    val reportingClass = entry.substring(29, 48)
    println("Reporting class: "+reportingClass)
    val report = entry.substring(50)
    println("Report: "+report)


  }

  private def loadDriver(className: String): Unit = try {
    Class.forName(className)
    println("monetdb driver loaded")
  } catch {
    case _: Throwable => println("could not load monetdb driver")
  }

  def getConnection(url: String, user: String, pass: String): Connection = {
    DriverManager.getConnection(url, user, pass)
  }

  def createTable(): Unit = {
    implicit val conn = getConnection("jdbc:monetdb://localhost/freamon", "monetdb", "monetdb")
    AuditLogModel.createTable()
  }
  def start(args: String): Unit = {
    import java.io.{BufferedReader, File, InputStreamReader}

    val logFile = new File(args)
    val br = new BufferedReader(new InputStreamReader(follow(logFile)))

    def read: Unit = {
      val l = br.readLine
      if(l != null){
        println(l)
        processEntry(l)
        read
      }
    }
    read
  }
  def follow(file: File): InputStream = {
    val maxRetries = 3
    val waitToOpen = 1000
    val waitBetweenReads = 100

    def sleep(msec: Long) = () => Thread.sleep(msec)

    follow(file, maxRetries, sleep(waitToOpen), sleep(waitBetweenReads))
  }

  def follow(file: File, openTries: Int, openSleep: () => Unit, rereadSleep: () => Unit): InputStream = {
    import java.io.SequenceInputStream

    val e = new java.util.Enumeration[InputStream]() {
      def nextElement = new FollowingInputStream(file, rereadSleep)
      def hasMoreElements = testExists(file, openTries, openSleep)
    }

    new SequenceInputStream(e)
  }

  def testExists(file: File, tries: Int, sleep: () => Unit): Boolean = {
    def tryExists(n: Int): Boolean =
      if (file.exists) true
      else if (n > tries) false
      else {
        sleep()
        tryExists(n+1)
      }
    tryExists(1)
  }

}

import java.io.{File, InputStream}

class FollowingInputStream(val file: File, val waitForNewInput: () => Unit) extends InputStream{

  import java.io.FileInputStream
  private val underlying = new FileInputStream(file)

  def read: Int = handle(underlying.read)

  override def read(b: Array[Byte]): Int = read(b, 0, b.length)

  override def read(b: Array[Byte], off: Int, len: Int): Int = handle(underlying.read(b, off, len))

  override def close(): Unit = underlying.close

  protected def rotated_? = try { underlying.getChannel.position() > file.length }

  protected def closed_? = !underlying.getChannel.isOpen

  protected def handle(read: => Int): Int = read match {
    case -1 if rotated_? || closed_? => -1
    case -1 =>
      waitForNewInput()
      handle(read)
    case i => i
  }

  require(file != null)
  assume(file.exists)

}


