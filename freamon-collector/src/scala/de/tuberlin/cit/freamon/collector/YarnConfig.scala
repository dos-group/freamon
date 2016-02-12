package de.tuberlin.cit.freamon.collector

import java.io.IOException

/**
 * Helper for retrieving certain settings from yarn-site.xml
 */
class YarnConfig {
  var configPath: String = null
  var cgroupsMountPath: String = null
  var cgroupsHierarchy: String = null

  def this(configPath: String) {
    this()

    try {
      val file = scala.xml.XML.loadFile(configPath)
      val properties = file.child.filter(_.label.toLowerCase == "property").map { property =>
        val key = property.child.find(_.label.toLowerCase == "name").get.text
        val value = property.child.find(_.label.toLowerCase == "value").get.text
        (key, value)
      }.toMap

      this.cgroupsMountPath = properties.getOrElse("yarn.nodemanager.linux-container-executor.cgroups.mount-path", "/sys/fs/cgroup")
      this.cgroupsHierarchy = properties.getOrElse("yarn.nodemanager.linux-container-executor.cgroups.hierarchy", "hadoop-yarn")

      this.configPath = configPath
    }
    catch {
      case e: IOException =>
        throw new IOException("Could not load file: " + configPath, e)
      case e: Throwable =>
        throw new IOException("Wrong format: " + configPath, e)
    }
  }

}
