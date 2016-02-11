package de.tuberlin.cit.freamon.collector

import scala.util.parsing.json.JSON

class Flink(appId: String) {

  var apiAddress = "http://localhost:8088/proxy"
  var apiTaskmanagersAddress = "taskmanagers"
  var taskManagerStats: Map[String, TaskManagerStats] = null

  def pollTaskmanagersApi(): Map[String, TaskManagerStats] = {
    val url = String.join("/", apiAddress, appId, apiTaskmanagersAddress)
    val jsonText = scala.io.Source.fromURL(url, "utf-8").mkString
    val jsonRoot = JSON.parseFull(jsonText).get.asInstanceOf[Map[String, Any]]
    val taskManagersJson = jsonRoot.get("taskmanagers").get.asInstanceOf[List[Map[String, Any]]]

    taskManagerStats = taskManagersJson.map { taskManagerJson =>
      val taskManager = new TaskManagerStats(taskManagerJson)
      (taskManager.instanceId, taskManager)
    }.toMap

    taskManagerStats
  }

}

class TaskManagerStats() {
  var instanceId: String = null
  var actorPath: String = null
  var dataPort: Double = -1
  var timeSinceLastHeartbeat: Double = -1
  var totalSlots: Double = -1
  var availableSlots: Double = -1
  var cpuCores: Double = -1
  var physicalMemory: Double = -1
  var freeMemory: Double = -1
  var managedMemory: Double = -1

  def this(jsonTaskManager: Map[String, Any]) {
    this()

    def get[T](key: String) = jsonTaskManager.get(key).get.asInstanceOf[T]

    instanceId = get[String]("id")
    actorPath = get[String]("path")
    dataPort = get[Double]("dataPort")
    timeSinceLastHeartbeat = get[Double]("timeSinceLastHeartbeat")
    totalSlots = get[Double]("slotsNumber")
    availableSlots = get[Double]("freeSlots")
    cpuCores = get[Double]("cpuCores")
    physicalMemory = get[Double]("physicalMemory")
    managedMemory = get[Double]("managedMemory")
    freeMemory = get[Double]("freeMemory")
  }
}
