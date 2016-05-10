package de.tuberlin.cit.freamon.collector

import org.scalamock.scalatest.MockFactory
import org.scalatest.FlatSpec

class CgroupSpec extends FlatSpec with MockFactory {
  val mountPath = getClass.getResource("/sys-fs-cgroup").getPath
  val groupId = "hadoop-yarn/container_1455551433868_0002_01_123456"

  def withCgroup(testCode: (Cgroup) => Any) {
    val cgroup = new Cgroup(mountPath, groupId)
    testCode(cgroup)
  }

  "getBlockDevice" should "return block device as x:y" in withCgroup { cgroup =>
    assertResult("253:1")(cgroup.getBlockDevice("/data", getClass.getResource("/proc/self/mountinfo").getPath))
  }

  "getCurrentBlockIOUsage" should "return total used bytes" in withCgroup { cgroup =>
    assertResult(104333312)(cgroup.getCurrentBlockIOUsage("253:1"))
  }

  "getAvgNetworkUsage" should "return always 0" in withCgroup { cgroup =>
    assertResult(0)(cgroup.getAvgNetworkUsage)
  }
}
