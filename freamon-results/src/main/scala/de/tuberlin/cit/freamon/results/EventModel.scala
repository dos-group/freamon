package de.tuberlin.cit.freamon.results

import java.time.Instant

/** Model class for experiment events extracted from the experiment run logs.
  *
  * @param appId The ID of the associated run.
  * @param name The name of the event.
  * @param timestamp The timestamp for this event.
  * @param value The double value for this event.
  */
case class EventModel(
                       appId: Int,
                       name: Symbol,
                       timestamp: Instant,
                       value: Double
                       ) {
  val id = this.##
}

/** [[EventModel]] companion and storage manager. */
object EventModel extends PersistedAPI[EventModel] {

  import java.sql.Connection

  import anorm.SqlParser._
  import anorm._

  override val tableName: String = "experiment_event"

  override val rowParser = {
    get[Int]     ("id")        ~
    get[Int]     ("app_id")    ~
    get[String]  ("name")      ~
    get[Instant] ("timestamp") ~
    get[Double]  ("value")    map {
      case id ~ expRunID ~ name ~ timestamp ~ value => EventModel(
        expRunID,
        Symbol(name),
        timestamp,
        value)
    }
  }

  override def createTable()(implicit conn: Connection): Unit = if (!tableExists) {
    SQL"""
      CREATE TABLE experiment_event (
        id        INTEGER     NOT NULL,
        app_id    INTEGER     NOT NULL,
        name      VARCHAR(63) NOT NULL,
        timestamp TIMESTAMP           ,
        value     DOUBLE              ,
        PRIMARY KEY (id),
        FOREIGN KEY (app_id) REFERENCES experiment_run(id) ON DELETE CASCADE
      )""".execute()
  }

  private val fields = s"""id, app_id, name, timestamp, value"""

  override def insert(x: EventModel)(implicit conn: Connection): Unit = {
    SQL"""
    INSERT INTO experiment_event($fields) VALUES(
      ${x.id},
      ${x.appId},
      ${x.name.name},
      ${x.timestamp},
      ${x.value}
    )
    """.executeInsert()
  }

  override def insert(xs: Seq[EventModel])(implicit conn: Connection): Unit = if (xs.nonEmpty) singleCommit {
    BatchSql(
      s"""
      INSERT INTO experiment_event($fields) VALUES(
        {id},
        {appId},
        {name},
        {timestamp},
        {value}
      )
      """,
      namedParametersFor(xs.head),
      xs.tail.map(namedParametersFor): _*
    ).execute()
  }

  override def update(x: EventModel)(implicit conn: Connection): Unit = {
    throw new NotImplementedError("EventModel objects are immutable, update is not supported")
  }

  override def delete(x: EventModel)(implicit conn: Connection): Unit = {
    SQL"""
    DELETE FROM experiment_event WHERE id = ${x.id}
    """.execute()
  }

  def namedParametersFor(x: EventModel): Seq[NamedParameter] = Seq[NamedParameter](
    'id         -> x.id,
    'appId      -> x.appId,
    'name       -> x.name.name,
    'timestamp -> x.timestamp,
    'value      -> x.value
  )
}