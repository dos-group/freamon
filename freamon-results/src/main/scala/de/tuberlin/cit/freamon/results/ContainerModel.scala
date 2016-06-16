package de.tuberlin.cit.freamon.results

/** Model class for the containers on which a job runs. */
case class ContainerModel(
                           containerId: String,
                           jobId: Int,
                           hostname: String
                           ) {
  val id = this.##
}

/** [[ContainerModel]] companion and storage manager. */
object ContainerModel extends PersistedAPI[ContainerModel] {

  import java.sql.Connection

  import anorm.SqlParser._
  import anorm._

  override val tableName: String = "experiment_container"

  override val rowParser = {
    get[Int]    ("id")           ~
    get[String] ("container_id") ~
    get[Int]    ("job_id")       ~
    get[String] ("hostname")     map {
      case id ~ containerId ~ jobId ~ hostname
      => ContainerModel(
        containerId,
        jobId,
        hostname)
    }
  }

  override def createTable()(implicit conn: Connection): Unit = if (!tableExists) {
    SQL(s"""
      CREATE TABLE $tableName (
        id                   INTEGER     NOT NULL,
        container_id         VARCHAR(63) UNIQUE  ,
        job_id               INTEGER     NOT NULL,
        hostname             VARCHAR(63)         ,
        PRIMARY KEY (id),
        FOREIGN KEY (job_id) REFERENCES ${JobModel.tableName}(id) ON DELETE CASCADE
      )""").execute()
  }

  private val fields = "id, container_id, job_id, hostname"

  override def insert(x: ContainerModel)(implicit conn: Connection): Unit = {
    SQL(s"""
      INSERT INTO $tableName($fields) VALUES(
        '${x.id}',
        '${x.containerId}',
        '${x.jobId}',
        '${x.hostname}'
      )
    """).executeInsert()
  }

  override def insert(xs: Seq[ContainerModel])(implicit conn: Connection): Unit = if (xs.nonEmpty) singleCommit {
    BatchSql(
      s"""
      INSERT INTO $tableName($fields) VALUES(
        '{id}',
        '{container_id}',
        '{job_id}',
        '{hostname}'
      )
      """,
      namedParametersFor(xs.head),
      xs.tail.map(namedParametersFor): _*
    ).execute()
  }

  override def update(x: ContainerModel)(implicit conn: Connection): Unit = {
    throw new NotImplementedError("ContainerModel objects are immutable, update is not supported")
  }

  override def delete(x: ContainerModel)(implicit conn: Connection): Unit = {
    SQL(s"""
    DELETE FROM $tableName WHERE id = ${x.id}
    """).execute()
  }

  def namedParametersFor(x: ContainerModel): Seq[NamedParameter] = Seq[NamedParameter](
    'id           -> x.id,
    'container_id -> x.containerId,
    'job_id       -> x.jobId,
    'hostname     -> x.hostname
  )
}
