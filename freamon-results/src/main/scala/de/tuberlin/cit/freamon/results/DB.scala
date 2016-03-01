package de.tuberlin.cit.freamon.results

import java.sql.{Connection, DriverManager}

object DB {

  // try to load the MonetDB JDBC drivers, which currently lacks an autoload descriptor
  // see https://www.monetdb.org/bugzilla/show_bug.cgi?id=3748 for a proposed fix
  loadDriver("nl.cwi.monetdb.jdbc.MonetDriver")

  /** Silently tries to load a JDBC driver with the given `className`.
    *
    * @param className The FQName of the driver to be loaded.
    */
  private def loadDriver(className: String): Unit = try {
    Class.forName(className)
    println("monetdb driver loaded")
  } catch {
    case _: Throwable => println("could not load monetdb driver") // silently ignore exception
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
  }

  /** Initialize the database schema.
    *
    * @param conn The DB connection.
    */
  def createSchema()(implicit conn: Connection): Unit = {
    println(s"Creating table ${EventModel.tableName}")
    EventModel.createTable()
  }
}
