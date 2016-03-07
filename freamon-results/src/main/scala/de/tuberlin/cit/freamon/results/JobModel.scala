package de.tuberlin.cit.freamon.results

import java.time.Instant

/** Model class for job runs. */
case class JobModel(
                     id: Int,
                     appId: String,
                     framework: Symbol,
                     start: Instant,
                     stop: Instant,
                     numContainers: Int,
                     coresPerContainer: Int,
                     memoryPerContainer: Int)

/** [[JobModel]] companion and storage manager. */
object JobModel extends PersistedAPI[JobModel] {

  import java.sql.Connection

  import anorm.SqlParser._
  import anorm._

  override val tableName: String = "experiment_job"

  override val rowParser = {
    get[Int]     ("id")                   ~
    get[String]  ("app_id")               ~
    get[String]  ("framework")            ~
    get[Instant] ("start")                ~
    get[Instant] ("stop")                 ~
    get[Int]     ("num_containers")       ~
    get[Int]     ("cores_per_container")  ~
    get[Int]     ("memory_per_container") map {
      case id ~ appId ~ framework ~ start ~ stop ~ numContainers ~ coresPerContainer ~ memoryPerContainer
      => JobModel(
        id,
        appId,
        Symbol(framework),
        start,
        stop,
        numContainers,
        coresPerContainer,
        memoryPerContainer)
    }
  }

  override def createTable()(implicit conn: Connection): Unit = if (!tableExists) {
    SQL"""
      CREATE TABLE $tableName (
        id                   INTEGER     NOT NULL,
        app_id               VARCHAR(63)         ,
        framework            VARCHAR(63)         ,
        start                TIMESTAMP           ,
        stop                 TIMESTAMP           ,
        num_containers       INTEGER             ,
        cores_per_container  INTEGER             ,
        memory_per_container INTEGER             ,
        PRIMARY KEY (id)
      )""".execute()
  }

  private val fields = "id, app_id, framework, start, stop, num_containers, cores_per_container, memory_per_container"

  override def insert(x: JobModel)(implicit conn: Connection): Unit = {
    SQL"""
    INSERT INTO $tableName($fields) VALUES(
      ${x.id},
      ${x.appId},
      ${x.framework.name},
      ${x.start},
      ${x.stop},
      ${x.numContainers},
      ${x.coresPerContainer},
      ${x.memoryPerContainer}
    )
    """.executeInsert()
  }

  override def insert(xs: Seq[JobModel])(implicit conn: Connection): Unit = if (xs.nonEmpty) singleCommit {
    BatchSql(
      s"""
      INSERT INTO $tableName($fields) VALUES(
        {id},
        {app_id},
        {framework},
        {start},
        {stop},
        {num_containers},
        {cores_per_container},
        {memory_per_container}
      )
      """,
      namedParametersFor(xs.head),
      xs.tail.map(namedParametersFor): _*
    ).execute()
  }

  override def update(x: JobModel)(implicit conn: Connection): Unit = {
    SQL"""
    UPDATE $tableName SET
      id                   = ${x.id},
      app_id               = ${x.appId},
      framework            = ${x.framework.name},
      start                = ${x.start},
      stop                 = ${x.stop},
      num_containers       = ${x.numContainers},
      cores_per_container  = ${x.coresPerContainer},
      memory_per_container = ${x.memoryPerContainer}
    WHERE id = ${x.id}
    """.executeUpdate()
  }

  override def delete(x: JobModel)(implicit conn: Connection): Unit = {
    SQL"""
    DELETE FROM $tableName WHERE id = ${x.id}
    """.execute()
  }

  def namedParametersFor(x: JobModel): Seq[NamedParameter] = Seq[NamedParameter](
    'id                   -> x.id,
    'app_id               -> x.appId,
    'framework            -> x.framework.name,
    'start                -> x.start,
    'stop                 -> x.stop,
    'num_containers       -> x.numContainers,
    'cores_per_container  -> x.coresPerContainer,
    'memory_per_container -> x.memoryPerContainer
  )
}
