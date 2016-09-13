package de.tuberlin.cit.freamon.results

import java.sql.PreparedStatement

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
        id      INT       AUTO_INCREMENT,
        date    BIGINT    NOT NULL,
        allowed BOOLEAN   NOT NULL,
        ugi     TEXT      NOT NULL,
        ip      TEXT      NOT NULL,
        cmd     TEXT      NOT NULL,
        src     TEXT              ,
        dst    TEXT              ,
        perm    TEXT              ,
        proto   TEXT,
        PRIMARY KEY(id));
     """).execute()
  }

  private val fields = "date, allowed, ugi, ip, cmd, src, dst, perm, proto"

  override def insert(x: AuditLogModel)(implicit conn: Connection): Unit = {
    val sql: String = "INSERT INTO "+tableName+" ("+fields+") VALUES (?,?,?,?,?,?,?,?,?)"
    val pstmt: PreparedStatement = conn.prepareStatement(sql)
    pstmt.setLong(1, x.date)
    pstmt.setBoolean(2, x.allowed)
    pstmt.setString(3, x.ugi)
    pstmt.setString(4, x.ip)
    pstmt.setString(5, x.cmd)
    pstmt.setString(6, x.src)
    pstmt.setString(7, x.dst)
    pstmt.setString(8, x.perm)
    pstmt.setString(9, x.proto)
    pstmt.execute()
  }

  override def insert(xs: Seq[AuditLogModel])(implicit conn: Connection): Unit = if (xs.nonEmpty) singleCommit{
    val sql = "INSERT INTO "+tableName+"("+fields+") VALUES (?,?,?,?,?,?,?,?,?)"
    val pstmt: PreparedStatement = conn.prepareStatement(sql)
    xs.foreach(i => {
      pstmt.setLong(1, i.date)
      pstmt.setBoolean(2, i.allowed)
      pstmt.setString(3, i.ugi)
      pstmt.setString(4, i.ip)
      pstmt.setString(5, i.cmd)
      pstmt.setString(6, i.src)
      pstmt.setString(7, i.dst)
      pstmt.setString(8, i.perm)
      pstmt.setString(9, i.proto)
      pstmt.addBatch()
    }
    )
    conn.commit()
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
