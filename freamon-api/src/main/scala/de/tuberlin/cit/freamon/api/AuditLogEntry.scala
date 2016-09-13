package de.tuberlin.cit.freamon.api

case class AuditLogEntry(){
  val date:Long = 1L
  val allowed:Boolean = false
  val ugi:String = null
  val ip:String = null
  val cmd:String = null
  val src:String = null
  val dst:String = null
  val perm:String = null
  val proto:String = null
}
