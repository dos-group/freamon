package de.tuberlin.cit.freamon.results

/** Model class for the containers on which a job runs. */
case class WorkerModel(
                        jobId: Int,
                        hostname: String,
                        isYarn: Boolean,
                        containerId: String = null
                      ) {
  val id = this.##
}

/** [[WorkerModel]] companion and storage manager. */
object WorkerModel extends PersistedAPI[WorkerModel] {

  import java.sql.Connection

  import anorm.SqlParser._
  import anorm._

  override val tableName: String = "worker"

  override val rowParser = {
    get[Int]    ("id")           ~
    get[Int]    ("job_id")       ~
    get[String] ("hostname")     ~
    get[Boolean] ("isYarn")      ~
    get[String] ("container_id") map {
      case id ~ jobId ~ hostname ~ isYarn ~ containerId
      => WorkerModel(
        jobId,
        hostname,
        isYarn,
        containerId
      )
    }
  }

  override def createTable()(implicit conn: Connection): Unit = if (!tableExists) {
    SQL(s"""
      CREATE TABLE $tableName (
        id                   INTEGER     NOT NULL,
        job_id               INTEGER     NOT NULL,
        hostname             VARCHAR(63)         ,
        isYarn               BOOLEAN             ,
        container_id         VARCHAR(63)         ,
        PRIMARY KEY (id),
        FOREIGN KEY (job_id) REFERENCES ${JobModel.tableName}(id) ON DELETE CASCADE
      )""").execute()
  }

  private val fields = "id, job_id, hostname, container_id"

  override def insert(x: WorkerModel)(implicit conn: Connection): Unit = {
    SQL(s"""
      INSERT INTO $tableName($fields) VALUES(
        '${x.id}',
        '${x.jobId}',
        '${x.hostname}',
        '${x.isYarn}',
        '${x.containerId}'
      )
    """).executeInsert()
  }

  override def insert(xs: Seq[WorkerModel])(implicit conn: Connection): Unit = if (xs.nonEmpty) singleCommit {
    BatchSql(
      s"""
      INSERT INTO $tableName($fields) VALUES(
        '{id}',
        '{job_id}',
        '{hostname}',
        '{isYarn}',
        '{container_id}'
      )
      """,
      namedParametersFor(xs.head),
      xs.tail.map(namedParametersFor): _*
    ).execute()
  }

  override def update(x: WorkerModel)(implicit conn: Connection): Unit = {
    throw new NotImplementedError("ContainerModel objects are immutable, update is not supported")
  }

  override def delete(x: WorkerModel)(implicit conn: Connection): Unit = {
    SQL(s"""
    DELETE FROM $tableName WHERE id = ${x.id}
    """).execute()
  }

  def namedParametersFor(x: WorkerModel): Seq[NamedParameter] = Seq[NamedParameter](
    'id           -> x.id,
    'job_id       -> x.jobId,
    'hostname     -> x.hostname,
    'isYarn       -> x.isYarn,
    'container_id -> x.containerId
  )
}
