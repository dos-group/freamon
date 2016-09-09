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
      start(args(3))
    }
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
