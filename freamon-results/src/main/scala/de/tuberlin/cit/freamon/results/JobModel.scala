package de.tuberlin.cit.freamon.results

import java.util.UUID

/** Model class for job runs. */
case class JobModel(
                     yarnAppId: String = null,
                     framework: Symbol = Symbol(null),
                     signature: String = null,
                     inputSize: Double = 0d,
                     numContainers: Int = 0,
                     coresPerContainer: Int = 0,
                     memoryPerContainer: Int = 0,
                     start: Long = System.currentTimeMillis(),
                     stop: Long = 0,
                     id: Int = UUID.randomUUID().hashCode()
                   ) {
}

/** [[JobModel]] companion and storage manager. */
object JobModel extends PersistedAPI[JobModel] {

  import java.sql.Connection

  import anorm.SqlParser._
  import anorm._

  override val tableName: String = "job"

  override val rowParser = {
    get[Int]     ("id")                   ~
    get[String]  ("yarn_application_id")  ~
    get[String]  ("framework")            ~
    get[String]  ("signature")            ~
    get[Double]  ("input_size")           ~
    get[Int]     ("num_containers")       ~
    get[Int]     ("cores_per_container")  ~
    get[Int]     ("memory_per_container") ~
    get[Long]    ("start")                ~
    get[Long]    ("stop")                 map {
      case id ~ appId ~ framework ~ signature ~ datasetSize ~ numContainers ~ coresPerContainer ~ memoryPerContainer ~ start ~ stop
      =>
        JobModel(
          appId,
          Symbol(framework),
          signature,
          datasetSize,
          numContainers,
          coresPerContainer,
          memoryPerContainer,
          start,
          stop,
          id)
    }
  }

  override def createTable()(implicit conn: Connection): Unit = if (!tableExists) {
    SQL(s"""
      CREATE TABLE $tableName (
        id                   INTEGER     NOT NULL,
        yarn_application_id  VARCHAR(63)         ,
        framework            VARCHAR(63)         ,
        signature            VARCHAR(255)        ,
        input_size           DOUBLE              ,
        num_containers       INTEGER             ,
        cores_per_container  INTEGER             ,
        memory_per_container INTEGER             ,
        start                BIGINT              ,
        stop                 BIGINT              ,
        PRIMARY KEY (id)
      )""").execute()
  }

  private val fields = "id, yarn_application_id, framework, signature, input_size, num_containers, cores_per_container, memory_per_container, start, stop"

  override def insert(x: JobModel)(implicit conn: Connection): Unit = {
    SQL(s"""
      INSERT INTO $tableName($fields) VALUES(
        '${x.id}',
        '${x.yarnAppId}',
        '${x.framework.name}',
        '${x.signature}',
        '${x.inputSize}',
        '${x.numContainers}',
        '${x.coresPerContainer}',
        '${x.memoryPerContainer}',
        '${x.start}',
        '${x.stop}'
      )
    """).executeInsert()
  }

  override def insert(xs: Seq[JobModel])(implicit conn: Connection): Unit = if (xs.nonEmpty) singleCommit {
    BatchSql(
      s"""
      INSERT INTO $tableName($fields) VALUES(
        '{id}',
        '{yarn_application_id}',
        '{framework}',
        '{signature}',
        '{input_size}',
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
      yarn_application_id  = '${x.yarnAppId}',
      framework            = '${x.framework.name}',
      signature            = '${x.signature}',
      input_size           = '${x.inputSize}',
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
    'yarn_application_id  -> x.yarnAppId,
    'framework            -> x.framework.name,
    'signature            -> x.signature,
    'input_size           -> x.inputSize,
    'num_containers       -> x.numContainers,
    'cores_per_container  -> x.coresPerContainer,
    'memory_per_container -> x.memoryPerContainer,
    'start                -> x.start,
    'stop                 -> x.stop
  )
}
