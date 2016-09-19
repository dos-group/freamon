package de.tuberlin.cit.freamon.api

/*case class AuditLogEntry(){
  var date: Long = 1L
  var allowed:Boolean = false
  var ugi:String = null
  var ip:String = null
  var cmd:String = null
  var src:String = null
  var dst:String = null
  var perm:String = null
  var proto:String = null
}*/

case class AuditLogEntry(var date: Long, var allowed: Boolean, var ugi: String, var ip: String, var cmd: String, var src: String, var dst: String, var perm: String, var proto: String)
