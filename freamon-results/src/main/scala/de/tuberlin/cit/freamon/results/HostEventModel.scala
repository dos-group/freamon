package de.tuberlin.cit.freamon.results

case class HostEventModel(
                         hostname: String,
                         kind: Symbol,
                         millis: Long,
                         value: Double
                         )

object HostEventModel extends PersistedAPI[HostEventModel]{

  import java.sql.Connection
  import anorm.SqlParser._
  import  anorm._

  override val tableName: String = "host_event"

  override val rowParser: RowParser[HostEventModel] = {
    get[String]     ("hostname")        ~
    get[String]  ("kind")              ~
    get[Long]    ("millis")            ~
    get[Double]  ("value")             map {
      case hostname ~ kind ~ millis ~ value => HostEventModel(
        hostname,
        Symbol(kind),
        millis,
        value
      )
    }
  }

  override def createTable()(implicit conn: Connection) : Unit = if (!tableExists) {
    SQL(
      s"""
         CREATE TABLE $tableName (
         hostname            VARCHAR(63)     NOT NULL,
         kind                VARCHAR(63) NOT NULL,
         millis              BIGINT      NOT NULL,
         value               DOUBLE      NOT NULL,
         PRIMARY KEY (hostname)
       """).execute()
  }

  private val fields = "hostname, kind, millis, value"

  override def insert(x: HostEventModel)(implicit conn: Connection) : Unit = {
    SQL(
      s"""
         INSERT INTO $tableName($fields) VALUES(
         '${x.hostname}',
         '${x.kind.name},
         '${x.millis},
         '${x.value}
       """).executeInsert()
  }

  override def insert(xs: Seq[HostEventModel])(implicit conn: Connection): Unit = if (xs.nonEmpty) singleCommit {
    BatchSql(
      s"""
         INSERT INTO $tableName($fields) VALUES(
         '{hostname}',
         '{kind}',
         '{millis}',
         '{value}'
       """,
      namedParametersFor(xs.head),
      xs.tail.map(namedParametersFor): _*
    ).execute()
  }

  override def update(x: HostEventModel)(implicit conn: Connection): Unit = {
    throw new NotImplementedError("HostEventUnit objects are immutable, update is not supported")
  }

  override def delete(x: HostEventModel)(implicit conn: Connection): Unit = {
    SQL(
      s"""
         DELETE FROM $tableName WHERE host_unit_id = ${x.hostname}
       """).execute()
  }

  def namedParametersFor(x: HostEventModel): Seq[NamedParameter] = Seq[NamedParameter](
    'host_unit_id    -> x.hostname,
    'kind         -> x.kind.name,
    'millis       -> x.millis,
    'value        -> x.value
  )

}
