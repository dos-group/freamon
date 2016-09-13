package de.tuberlin.cit.freamon.api

case class AuditLogEntry(){
  val date = new Long
  val allowed = new Boolean
  val ugi = new String
  val ip = new String
  val cmd = new String
  val src = new String
  val dst = new String
  val perm = new String
  val proto = new String
}
