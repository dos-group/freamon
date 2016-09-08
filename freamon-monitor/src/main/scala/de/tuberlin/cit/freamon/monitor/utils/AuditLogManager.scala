package de.tuberlin.cit.freamon.monitor.utils

import de.tuberlin.cit.freamon.collector.AuditLogCollector
import de.tuberlin.cit.freamon.results.AuditLogModel
import java.sql.{Connection, DriverManager}

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
      AuditLogCollector.start(args(3))
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

}
