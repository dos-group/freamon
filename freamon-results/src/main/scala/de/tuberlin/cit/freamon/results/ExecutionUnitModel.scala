package de.tuberlin.cit.freamon.results

/** Model class for the execution units on which a job runs
  * (both containers and master, or whole node on standalone) */
case class ExecutionUnitModel(
                               jobId: Int,
                               hostname: String,
                               isYarnContainer: Boolean,
                               containerId: String = null,
                               isMaster: Boolean = false
                      ) {
  val id = this.##
}

/** [[ExecutionUnitModel]] companion and storage manager. */
object ExecutionUnitModel extends PersistedAPI[ExecutionUnitModel] {

  import java.sql.Connection

  import anorm.SqlParser._
  import anorm._

  override val tableName: String = "execution_unit"

  override val rowParser = {
    get[Int]     ("id")                ~
    get[Int]     ("job_id")            ~
    get[String]  ("hostname")          ~
    get[Boolean] ("is_yarn_container") ~
    get[Boolean] ("is_master")         ~
    get[String]  ("container_id")      map {
      case id ~ jobId ~ hostname ~ isYarn ~ isMaster ~ containerId
      => ExecutionUnitModel(
        jobId,
        hostname,
        isYarn,
        containerId,
        isMaster
      )
    }
  }

  override def createTable()(implicit conn: Connection): Unit = if (!tableExists) {
    SQL(s"""
      CREATE TABLE $tableName (
        id                   INTEGER     NOT NULL,
        job_id               INTEGER     NOT NULL,
        hostname             VARCHAR(63)         ,
        is_yarn_container    BOOLEAN             ,
        is_master            BOOLEAN             ,
        container_id         VARCHAR(63)         ,
        PRIMARY KEY (id, job_id),
        FOREIGN KEY (job_id) REFERENCES ${JobModel.tableName}(id) ON DELETE CASCADE
      )""").execute()
  }

  private val fields = "id, job_id, hostname, is_yarn_container, is_master, container_id"

  override def insert(x: ExecutionUnitModel)(implicit conn: Connection): Unit = {
    SQL(s"""
      INSERT INTO $tableName($fields) VALUES(
        '${x.id}',
        '${x.jobId}',
        '${x.hostname}',
        '${x.isYarnContainer}',
        '${x.isMaster}',
        '${x.containerId}'
      )
    """).executeInsert()
  }

  override def insert(xs: Seq[ExecutionUnitModel])(implicit conn: Connection): Unit = if (xs.nonEmpty) singleCommit {
    BatchSql(
      s"""
      INSERT INTO $tableName($fields) VALUES(
        '{id}',
        '{job_id}',
        '{hostname}',
        '{is_yarn_container}',
        '{is_master}',
        '{container_id}'
      )
      """,
      namedParametersFor(xs.head),
      xs.tail.map(namedParametersFor): _*
    ).execute()
  }

  override def update(x: ExecutionUnitModel)(implicit conn: Connection): Unit = {
    throw new NotImplementedError("ContainerModel objects are immutable, update is not supported")
  }

  override def delete(x: ExecutionUnitModel)(implicit conn: Connection): Unit = {
    SQL(s"""
    DELETE FROM $tableName WHERE id = ${x.id}
    """).execute()
  }

  def namedParametersFor(x: ExecutionUnitModel): Seq[NamedParameter] = Seq[NamedParameter](
    'id           -> x.id,
    'job_id       -> x.jobId,
    'hostname     -> x.hostname,
    'is_yarn_container -> x.isYarnContainer,
    'is_master    -> x.isMaster,
    'container_id -> x.containerId
  )
}
