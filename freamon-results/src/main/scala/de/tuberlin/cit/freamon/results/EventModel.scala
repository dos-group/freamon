package de.tuberlin.cit.freamon.results

import java.time.Instant

/** Model class for experiment events collected by the master actor from the agent actors.
  *
  * @param jobId The ID of the associated job run.
  * @param kind The event type.
  * @param timestamp The timestamp for this event.
  * @param value The double value for this event.
  */
case class EventModel(
                       jobId: Int,
                       kind: Symbol,
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
    get[Int]     ("job_id")    ~
    get[String]  ("kind")      ~
    get[Instant] ("timestamp") ~
    get[Double]  ("value")    map {
      case id ~ expRunID ~ kind ~ timestamp ~ value => EventModel(
        expRunID,
        Symbol(kind),
        timestamp,
        value)
    }
  }

  override def createTable()(implicit conn: Connection): Unit = if (!tableExists) {
    SQL(s"""
      CREATE TABLE $tableName (
        id        INTEGER     NOT NULL,
        job_id    INTEGER     NOT NULL,
        kind      VARCHAR(63) NOT NULL,
        timestamp TIMESTAMP           ,
        value     DOUBLE              ,
        PRIMARY KEY (id),
        FOREIGN KEY (job_id) REFERENCES ${JobModel.tableName}(id) ON DELETE CASCADE
      )""").execute()
  }

  private val fields = "id, job_id, kind, timestamp, value"

  override def insert(x: EventModel)(implicit conn: Connection): Unit = {
    SQL(s"""
    INSERT INTO $tableName($fields) VALUES(
      '${x.id}',
      '${x.jobId}',
      '${x.kind.name}',
      '${x.timestamp}',
      '${x.value}'
    )
    """).executeInsert()
  }

  override def insert(xs: Seq[EventModel])(implicit conn: Connection): Unit = if (xs.nonEmpty) singleCommit {
    BatchSql(
      s"""
      INSERT INTO $tableName($fields) VALUES(
        '{id}',
        '{job_id}',
        '{kind}',
        '{timestamp}',
        '{value}'
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
    SQL(s"""
    DELETE FROM $tableName WHERE id = ${x.id}
    """).execute()
  }

  def namedParametersFor(x: EventModel): Seq[NamedParameter] = Seq[NamedParameter](
    'id        -> x.id,
    'job_id    -> x.jobId,
    'kind      -> x.kind.name,
    'timestamp -> x.timestamp,
    'value     -> x.value
  )
}
