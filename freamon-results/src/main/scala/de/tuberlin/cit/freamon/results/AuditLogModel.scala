package de.tuberlin.cit.freamon.results

/**
  * Model class for HDFS Audit logs collected by the master actor from agent actors
  */
case class AuditLogModel(
                          date: Long,
                          allowed: Boolean,
                          ugi: String,
                          ip: String,
                          cmd: String,
                          src: String,
                          dst: String,
                          perm: String,
                          proto: String
                        )

object AuditLogModel extends PersistedAPI[AuditLogModel]{

  import java.sql.Connection
  import anorm.SqlParser._
  import anorm._

  override val tableName: String = "audit_log_entry"

  override val rowParser = {
    get[Long]     ("date")    ~
      get[Boolean]  ("allowed") ~
      get[String]   ("ugi")     ~
      get[String]   ("ip")      ~
      get[String]   ("cmd")     ~
      get[String]   ("src")     ~
      get[String]   ("dst")    ~
      get[String]   ("perm")    ~
      get[String]   ("proto")     map{
      case date ~ allowed ~ ugi ~ ip ~ cmd ~ src ~ dst ~ perm ~ proto => AuditLogModel(
        date,
        allowed,
        ugi,
        ip,
        cmd,
        src,
        dst,
        perm,
        proto)
    }
  }

  override def createTable()(implicit conn: Connection): Unit = if(!tableExists) {
    SQL(
      s"""
       CREATE TABLE $tableName (
        date    BIGINT    UNIQUE,
        allowed BOOLEAN   NOT NULL,
        ugi     TEXT      NOT NULL,
        ip      TEXT      NOT NULL,
        cmd     TEXT      NOT NULL,
        src     TEXT              ,
        dst    TEXT              ,
        perm    TEXT              ,
        proto   TEXT);
     """).execute()
  }

  private val fields = "date, allowed, ugi, ip, cmd, src, dst, perm, proto"

  override def insert(x: AuditLogModel)(implicit conn: Connection): Unit = {
    SQL(
      s"""
         INSERT INTO $tableName($fields) VALUES(
         '${x.date}',
         '${x.allowed}',
         '${x.ugi},
         '${x.ip}',
         '${x.cmd}',
         '${x.src},
         '${x.dst}',
         '${x.perm}',
         '${x.proto}'
         );
       """).executeInsert()
    val sql = SQL(
      s"""
         INSERT INTO $tableName($fields) VALUES(
         '${x.date}',
         '${x.allowed}',
         '${x.ugi},
         '${x.ip}',
         '${x.cmd}',
         '${x.src},
         '${x.dst}',
         '${x.perm}',
         '${x.proto}'
         );
       """)
    println(sql)
  }

  override def insert(xs: Seq[AuditLogModel])(implicit conn: Connection): Unit = if (xs.nonEmpty) singleCommit{
    BatchSql(
      s"""
         INSERT INTO $tableName($fields) VALUES(
         '{date}',
         '{allowed}',
         '{ugi}',
         '{ip}',
         '{cmd}',
         '{src}',
         '{dst}',
         '{perm}',
         '{proto}'
         );
       """,
      namedParametrsFor(xs.head),
      xs.tail.map(namedParametrsFor): _*
    ).execute()
  }

  override def update(x: AuditLogModel)(implicit conn: Connection): Unit = {
    throw new NotImplementedError("AuditLogModel objects are immutable, updates are not supported")
  }

  override def delete(x: AuditLogModel)(implicit conn: Connection): Unit = {
    throw new NotImplementedError("AuditLogModel objects cannot be deleted")
  }


  def namedParametrsFor(x: AuditLogModel): Seq[NamedParameter] = Seq[NamedParameter](
    'date -> x.date,
    'allowed -> x.allowed,
    'ugi -> x.ugi,
    'ip -> x.ip,
    'cmd -> x.cmd,
    'src -> x.src,
    'dst -> x.dst,
    'perm -> x.perm,
    'proto -> x.proto
  )


}
