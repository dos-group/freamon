package de.tuberlin.cit.freamon.results

import java.sql.{Connection, DriverManager}

object DB {

  // try to load the MonetDB JDBC drivers, which currently lacks an autoload descriptor
  // see https://www.monetdb.org/bugzilla/show_bug.cgi?id=3748 for a proposed fix
  loadDriver("nl.cwi.monetdb.jdbc.MonetDriver")

  /** Tries to load a JDBC driver with the given `className`.
    *
    * @param className The FQName of the driver to be loaded.
    */
  private def loadDriver(className: String): Unit = try {
    Class.forName(className)
    println("monetdb driver loaded")
  } catch {
    case _: Throwable => println("could not load monetdb driver")
  }

  def main(args: Array[String]) {
    implicit val conn = getConnection("jdbc:monetdb://localhost/freamon", "monetdb", "monetdb")
    createSchema()
    val applicationId = s"application_${(System.currentTimeMillis() / 1000).asInstanceOf[Int]}_0001"
    val job = JobModel(applicationId, 'Spark, "test.jar", 0, 0, 0, 0, System.currentTimeMillis())

    JobModel.insert(job)
    JobModel.insert(JobModel())
    println(JobModel.selectAll().mkString("\n"))

    val eu1 = ExecutionUnitModel(job.id, "localhost", isYarnContainer = true, "1")
    val eu2 = ExecutionUnitModel(job.id, "localhost", isYarnContainer = true, "2")
    println(s"execUnit ids: ${eu1.id} ${eu2.id}")
    ExecutionUnitModel.insert(eu1)
    ExecutionUnitModel.insert(eu2)
    println(ExecutionUnitModel.selectAll().mkString("\n"))

    EventModel.insert(EventModel(eu1.id, job.id, 'cpu, System.currentTimeMillis(), 1))
    EventModel.insert(EventModel(eu2.id, job.id, 'cpu, System.currentTimeMillis(), 1))
    EventModel.insert(EventModel(eu1.id, job.id, 'cpu, System.currentTimeMillis(), 0))
    EventModel.insert(EventModel(eu2.id, job.id, 'cpu, System.currentTimeMillis(), 0.5))
    EventModel.insert(EventModel(eu1.id, job.id, 'cpu, System.currentTimeMillis(), 1))
    EventModel.insert(EventModel(eu2.id, job.id, 'cpu, System.currentTimeMillis(), 0))

    EventModel.insert(EventModel(eu1.id, job.id, 'mem, System.currentTimeMillis(), 123123))
    EventModel.insert(EventModel(eu1.id, job.id, 'netRx, System.currentTimeMillis(), 1.23))
    EventModel.insert(EventModel(eu1.id, job.id, 'netTx, System.currentTimeMillis(), 1.98))
    println(EventModel.selectAll().mkString("\n"))

    val newJob: JobModel = JobModel.selectWhere(s"yarn_application_id = '$applicationId'").head
      .copy(stop = System.currentTimeMillis())
    println(s"updating job: $newJob with id ${newJob.id}")
    JobModel.update(newJob)

    println(JobModel.selectAll().mkString("\n"))
  }

  /** Creates a database connection.
    */
  def getConnection(url: String, user: String, pass: String): Connection = {
    DriverManager.getConnection(url, user, pass)
  }

  /** Drop the database schema.
    *
    * @param conn The DB connection.
    */
  def dropSchema()(implicit conn: Connection): Unit = {
    println(s"Dropping table ${EventModel.tableName}")
    EventModel.dropTable()
    println(s"Dropping table ${ExecutionUnitModel.tableName}")
    ExecutionUnitModel.dropTable()
    println(s"Dropping table ${JobModel.tableName}")
    JobModel.dropTable()
    println(s"Dropping table ${AuditLogModel.tableName}")
    AuditLogModel.dropTable()
    println(s"Dropping table ${HostEventModel.tableName}")
    HostEventModel.dropTable()
  }

  /** Initialize the database schema.
    *
    * @param conn The DB connection.
    */
  def createSchema()(implicit conn: Connection): Unit = {
    println(s"Creating table ${JobModel.tableName}")
    JobModel.createTable()
    println(s"Creating table ${ExecutionUnitModel.tableName}")
    ExecutionUnitModel.createTable()
    println(s"Creating table ${EventModel.tableName}")
    EventModel.createTable()
    println(s"Creating table ${AuditLogModel.tableName}")
    AuditLogModel.createTable()
    println(s"Creating table ${HostEventModel.tableName}")
    HostEventModel.createTable()
  }
}
