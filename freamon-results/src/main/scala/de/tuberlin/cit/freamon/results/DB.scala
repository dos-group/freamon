package de.tuberlin.cit.freamon.results

import java.sql.{Connection, DriverManager}
import java.time.Instant

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
    val applicationId = s"application_${Instant.now().getEpochSecond}_0001"
    val job = JobModel(applicationId, 'Flink, 0, 0, 0, Instant.now())

    JobModel.insert(job)
    println(JobModel.selectAll().mkString("\n"))

    ContainerModel.insert(ContainerModel("1", job.id, "localhost"))
    ContainerModel.insert(ContainerModel("2", job.id, "localhost"))
    println(ContainerModel.selectAll().mkString("\n"))

    EventModel.insert(EventModel(1, job.id, 'cpu, Instant.now(), 0.42))
    EventModel.insert(EventModel(1, job.id, 'mem, Instant.now(), 123123))
    println(EventModel.selectAll().mkString("\n"))

    val newJob: JobModel = JobModel.selectWhere(s"app_id = '$applicationId'").head.copy(stop = Instant.now())
    println("updating job: " + newJob)
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
    println(s"Dropping table ${ContainerModel.tableName}")
    ContainerModel.dropTable()
    println(s"Dropping table ${JobModel.tableName}")
    JobModel.dropTable()
  }

  /** Initialize the database schema.
    *
    * @param conn The DB connection.
    */
  def createSchema()(implicit conn: Connection): Unit = {
    println(s"Creating table ${JobModel.tableName}")
    JobModel.createTable()
    println(s"Creating table ${ContainerModel.tableName}")
    ContainerModel.createTable()
    println(s"Creating table ${EventModel.tableName}")
    EventModel.createTable()
  }
}
