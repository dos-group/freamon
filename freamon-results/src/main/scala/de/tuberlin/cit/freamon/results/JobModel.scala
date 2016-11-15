package de.tuberlin.cit.freamon.results

import java.util.UUID

/** Model class for job runs. */
case class JobModel(
                     yarnAppId: String = null,
                     framework: Symbol = Symbol(null),
                     signature: String = null,
                     inputSize: Double = 0d,
                     numWorkers: Int = 0,
                     coresPerWorker: Int = 0,
                     memoryPerWorker: Int = 0,
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
    get[Int]     ("num_workers")          ~
    get[Int]     ("cores_per_worker")     ~
    get[Int]     ("memory_per_worker")    ~
    get[Long]    ("start")                ~
    get[Long]    ("stop")                 map {
      case id ~ appId ~ framework ~ signature ~ datasetSize ~ numWorkers ~ coresPerWorker ~ memoryPerWorker ~ start ~ stop
      =>
        JobModel(
          appId,
          Symbol(framework),
          signature,
          datasetSize,
          numWorkers,
          coresPerWorker,
          memoryPerWorker,
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
        num_workers          INTEGER             ,
        cores_per_worker     INTEGER             ,
        memory_per_worker    INTEGER             ,
        start                BIGINT              ,
        stop                 BIGINT              ,
        PRIMARY KEY (id)
      )""").execute()
  }

  private val fields = "id, yarn_application_id, framework, signature, input_size, num_workers, cores_per_worker, memory_per_worker, start, stop"

  override def insert(x: JobModel)(implicit conn: Connection): Unit = {
    SQL(s"""
      INSERT INTO $tableName($fields) VALUES(
        '${x.id}',
        '${x.yarnAppId}',
        '${x.framework.name}',
        '${x.signature}',
        '${x.inputSize}',
        '${x.numWorkers}',
        '${x.coresPerWorker}',
        '${x.memoryPerWorker}',
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
        '{num_workers}',
        '{cores_per_worker}',
        '{memory_per_worker}',
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
      num_workers          = '${x.numWorkers}',
      cores_per_worker     = '${x.coresPerWorker}',
      memory_per_worker    = '${x.memoryPerWorker}',
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
    'num_workers          -> x.numWorkers,
    'cores_per_worker     -> x.coresPerWorker,
    'memory_per_worker    -> x.memoryPerWorker,
    'start                -> x.start,
    'stop                 -> x.stop
  )
}
