package de.tuberlin.cit.freamon.results

import java.sql.{Connection, DriverManager}

import akka.event.LoggingAdapter

object DB {

  var logger: LoggingAdapter = null

  // try to load the MonetDB JDBC drivers, which currently lacks an autoload descriptor
  // see https://www.monetdb.org/bugzilla/show_bug.cgi?id=3748 for a proposed fix
  loadDriver("nl.cwi.monetdb.jdbc.MonetDriver")

  /** Silently tries to load a JDBC driver with the given `className`.
    *
    * @param className The FQName of the driver to be loaded.
    */
  private def loadDriver(className: String): Unit = try {
    Class.forName(className)
  } catch {
    case _: Throwable => // silently ignore exception
  }

  /** Creates a database connection using the 'app.db.\$connName.conf' connection data.
    */
  def getConnection(url: String, user: String, pass: String): Connection = {
    DriverManager.getConnection(url, user, pass)
  }

  /** Drop the database schema.
    *
    * @param conn The DB connection.
    */
  def dropSchema(silent: Boolean = false)(implicit conn: Connection): Unit = {
    if (!silent) logger.info(s"Dropping table ${ExperimentEvent.tableName}")
    ExperimentEvent.dropTable()
  }

  /** Initialize the database schema.
    *
    * @param conn The DB connection.
    */
  def createSchema(silent: Boolean = false)(implicit conn: Connection): Unit = {
    if (!silent) logger.info(s"Creating table ${ExperimentEvent.tableName}")
    ExperimentEvent.createTable()
  }
}
