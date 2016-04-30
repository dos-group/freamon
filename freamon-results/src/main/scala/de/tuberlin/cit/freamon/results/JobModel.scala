package de.tuberlin.cit.freamon.results

/** Model class for job runs. */
case class JobModel(
                     appId: String,
                     framework: Symbol,
                     numContainers: Int,
                     coresPerContainer: Int,
                     memoryPerContainer: Int,
                     start: Long = System.currentTimeMillis(),
                     stop: Long = 0
                     ) {
  val id = appId.##
}

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
    get[Int]     ("num_containers")       ~
    get[Int]     ("cores_per_container")  ~
    get[Int]     ("memory_per_container") ~
    get[Long]    ("start")                ~
    get[Long]    ("stop")                 map {
      case id ~ appId ~ framework ~ numContainers ~ coresPerContainer ~ memoryPerContainer ~ start ~ stop
      => JobModel(
        appId,
        Symbol(framework),
        numContainers,
        coresPerContainer,
        memoryPerContainer,
        start,
        stop)
    }
  }

  override def createTable()(implicit conn: Connection): Unit = if (!tableExists) {
    SQL(s"""
      CREATE TABLE $tableName (
        id                   INTEGER     NOT NULL,
        app_id               VARCHAR(63) UNIQUE  ,
        framework            VARCHAR(63)         ,
        num_containers       INTEGER             ,
        cores_per_container  INTEGER             ,
        memory_per_container INTEGER             ,
        start                BIGINT              ,
        stop                 BIGINT              ,
        PRIMARY KEY (id)
      )""").execute()
  }

  private val fields = "id, app_id, framework, num_containers, cores_per_container, memory_per_container, start, stop"

  override def insert(x: JobModel)(implicit conn: Connection): Unit = {
    val s: String = s"""
    INSERT INTO $tableName($fields) VALUES(
      '${x.id}',
      '${x.appId}',
      '${x.framework.name}',
      '${x.numContainers}',
      '${x.coresPerContainer}',
      '${x.memoryPerContainer}',
      '${x.start}',
      '${x.stop}'
    )
    """
    println(s)
    SQL(s).executeInsert()
  }

  override def insert(xs: Seq[JobModel])(implicit conn: Connection): Unit = if (xs.nonEmpty) singleCommit {
    BatchSql(
      s"""
      INSERT INTO $tableName($fields) VALUES(
        '{id}',
        '{app_id}',
        '{framework}',
        '{num_containers}',
        '{cores_per_container}',
        '{memory_per_container}',
        '{start}',
        '{stop}'
      )
      """,
      namedParametersFor(xs.head),
      xs.tail.map(namedParametersFor): _*
    ).execute()
  }

  override def update(x: JobModel)(implicit conn: Connection): Unit = {
    SQL(s"""
    UPDATE $tableName SET
      id                   = '${x.id}',
      app_id               = '${x.appId}',
      framework            = '${x.framework.name}',
      num_containers       = '${x.numContainers}',
      cores_per_container  = '${x.coresPerContainer}',
      memory_per_container = '${x.memoryPerContainer}',
      start                = '${x.start}',
      stop                 = '${x.stop}'
    WHERE id = ${x.id}
    """).executeUpdate()
  }

  override def delete(x: JobModel)(implicit conn: Connection): Unit = {
    SQL(s"""
    DELETE FROM $tableName WHERE id = ${x.id}
    """).execute()
  }

  def namedParametersFor(x: JobModel): Seq[NamedParameter] = Seq[NamedParameter](
    'id                   -> x.id,
    'app_id               -> x.appId,
    'framework            -> x.framework.name,
    'num_containers       -> x.numContainers,
    'cores_per_container  -> x.coresPerContainer,
    'memory_per_container -> x.memoryPerContainer,
    'start                -> x.start,
    'stop                 -> x.stop
  )
}
