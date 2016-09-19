package de.tuberlin.cit.freamon.api

/**
  * Created by arbeit on 19.09.16.
  */
case class AuditLogProperEntry(){
  var date: Long = 1L
  var allowed:Boolean = false
  var ugi:String = null
  var ip:String = null
  var cmd:String = null
  var src:String = null
  var dst:String = null
  var perm:String = null
  var proto:String = null
}
