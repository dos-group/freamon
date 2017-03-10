package de.tuberlin.cit.freamon.results

/** Model class for Spark stages. */
case class StageModel(
                     jobId: Int,
                     appSignature: String = null,
                     inputSize: Double = 0d,
                     stageNr: Int = 0,
                     numExecutors: Int = 0,
                     start: Long = System.currentTimeMillis(),
                     stop: Long = 0
                   ) {
  val id: Int = this.##
}

/** [[StageModel]] companion and storage manager. */
object StageModel extends PersistedAPI[StageModel] {

  import java.sql.Connection

  import anorm.SqlParser._
  import anorm._

  override val tableName: String = "stage"

  override val rowParser = {
    get[Int]     ("id")                   ~
    get[Int]     ("job_id")               ~
    get[String]  ("signature")            ~
    get[Double]  ("input_size")           ~
    get[Int]     ("stage_nr")             ~
    get[Int]     ("num_executors")        ~
    get[Long]    ("start")                ~
    get[Long]    ("stop")                 map {
      case id ~ jobId ~ signature ~ inputSize ~ stageNr ~ numExecutors ~ start ~ stop
      =>
        StageModel(
          jobId,
          signature,
          inputSize,
          stageNr,
          numExecutors,
          start,
          stop)
    }
  }

  override def createTable()(implicit conn: Connection): Unit = if (!tableExists) {
    SQL(s"""
      CREATE TABLE $tableName (
        id                   INTEGER     NOT NULL,
        signature            VARCHAR(255)        ,
        input_size           DOUBLE              ,
        stage_nr             INTEGER             ,
        num_executors        INTEGER             ,
        start                BIGINT              ,
        stop                 BIGINT              ,
        PRIMARY KEY (id),
        FOREIGN KEY (job_id) REFERENCES ${JobModel.tableName}(id) ON DELETE CASCADE
      )""").execute()
  }

  private val fields = "id, job_id, signature, input_size, stage_nr, num_executors, start, stop"

  override def insert(x: StageModel)(implicit conn: Connection): Unit = {
    SQL(s"""
      INSERT INTO $tableName($fields) VALUES(
        '${x.id}',
        '${x.jobId}',
        '${x.appSignature}',
        '${x.inputSize}',
        '${x.stageNr}',
        '${x.numExecutors}',
        '${x.start}',
        '${x.stop}'
      )
    """).executeInsert()
  }

  override def insert(xs: Seq[StageModel])(implicit conn: Connection): Unit = if (xs.nonEmpty) singleCommit {
    BatchSql(
      s"""
      INSERT INTO $tableName($fields) VALUES(
        '{id}',
        '{jobId}',
        '{appSignature}',
        '{inputSize}',
        '{stageNr}',
        '{numExecutors}',
        '{start}',
        '{stop}'
      )
      """,
      namedParametersFor(xs.head),
      xs.tail.map(namedParametersFor): _*
    ).execute()
  }

  override def update(x: StageModel)(implicit conn: Connection): Unit = {
    SQL(s"""
    UPDATE $tableName SET
      id            = '${x.id}',
      job_id        = '${x.jobId}',
      signature     = '${x.appSignature}',
      input_size    = '${x.inputSize}',
      stage_nr      = '${x.stageNr}',
      num_executors = '${x.numExecutors}',
      start         = '${x.start}',
      stop          = '${x.stop}'
    WHERE id = '${x.id}'
    """).executeUpdate()
  }

  override def delete(x: StageModel)(implicit conn: Connection): Unit = {
    SQL(s"""
    DELETE FROM $tableName WHERE id = ${x.id}
    """).execute()
  }

  def namedParametersFor(x: StageModel): Seq[NamedParameter] = Seq[NamedParameter](
    'id            -> x.id,
    'job_id        -> x.jobId,
    'signature     -> x.appSignature,
    'input_size    -> x.inputSize,
    'stage_nr      -> x.stageNr,
    'num_executors -> x.numExecutors,
    'start         -> x.start,
    'stop          -> x.stop
  )
}
