package de.tuberlin.cit.freamon.results

/** Model class for experiment events collected by the master actor from the agent actors.
  *
  * @param jobId The ID of the associated job run.
  * @param kind The event type.
  * @param millis The milliseconds after epoch for this event, as returned from System.currentTimeMillis().
  * @param value The double value for this event.
  */
case class EventModel(
                       execUnitId: Int,
                       jobId: Int,
                       kind: Symbol,
                       millis: Long,
                       value: Double)

/** [[EventModel]] companion and storage manager. */
object EventModel extends PersistedAPI[EventModel] {

  import java.sql.Connection

  import anorm.SqlParser._
  import anorm._

  override val tableName: String = "event"

  override val rowParser = {
    get[Int]     ("execution_unit_id") ~
    get[Int]     ("job_id")       ~
    get[String]  ("kind")         ~
    get[Long]    ("millis")       ~
    get[Double]  ("value")        map {
      case workerId ~ jobId ~ kind ~ millis ~ value => EventModel(
        workerId,
        jobId,
        Symbol(kind),
        millis,
        value)
    }
  }

  override def createTable()(implicit conn: Connection): Unit = if (!tableExists) {
    SQL(s"""
      CREATE TABLE $tableName (
        execution_unit_id INTEGER NOT NULL,
        job_id       INTEGER     NOT NULL,
        kind         VARCHAR(63) NOT NULL,
        millis       BIGINT              ,
        value        DOUBLE              ,
        FOREIGN KEY (execution_unit_id, job_id) REFERENCES ${ExecutionUnitModel.tableName}(id, job_id) ON DELETE CASCADE,
        FOREIGN KEY (job_id) REFERENCES ${JobModel.tableName}(id) ON DELETE CASCADE
      )""").execute()
  }

  private val fields = "execution_unit_id, job_id, kind, millis, value"

  override def insert(x: EventModel)(implicit conn: Connection): Unit = {
    SQL(s"""
    INSERT INTO $tableName($fields) VALUES(
      '${x.execUnitId}',
      '${x.jobId}',
      '${x.kind.name}',
      '${x.millis}',
      '${x.value}'
    )
    """).executeInsert()
  }

  override def insert(xs: Seq[EventModel])(implicit conn: Connection): Unit = if (xs.nonEmpty) singleCommit {
    BatchSql(
      s"""
      INSERT INTO $tableName($fields) VALUES(
        '{execution_unit_id}',
        '{job_id}',
        '{kind}',
        '{millis}',
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
    throw new NotImplementedError("EventModel deletion")
  }

  def namedParametersFor(x: EventModel): Seq[NamedParameter] = Seq[NamedParameter](
    'execution_unit_id    -> x.execUnitId,
    'job_id       -> x.jobId,
    'kind         -> x.kind.name,
    'millis       -> x.millis,
    'value        -> x.value
  )
}
