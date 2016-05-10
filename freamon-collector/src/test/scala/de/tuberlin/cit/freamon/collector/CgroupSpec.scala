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

  "parseNetworkUsage" should "sum up all rx and tx values" in withCgroup { cgroup =>
    val taskNetStatFile = getClass.getResource("/proc/1234/net/dev").getPath
    assert(cgroup.parseNetworkUsage(taskNetStatFile) == (15651 + 648))
  }

  "parseNetworkUsage" should "return 0 if file is missing (task finished)" in withCgroup { cgroup =>
    assert(cgroup.parseNetworkUsage("not-existing-path") == 0l)
  }

  "getCurrentNetworkUsage" should "return network usage of all container tasks" in {
    val parseMock = mockFunction[String, Long]
    parseMock.expects("/proc/12/net/dev").returns(10).twice // init + call = twice
    parseMock.expects("/proc/34/net/dev").returns(20).twice
    parseMock.expects("/proc/56/net/dev").returns(30).twice

    val cgroupMock = new Cgroup(mountPath, groupId) {
      override def parseNetworkUsage(path: String): Long = parseMock(path)
    }

    assert(cgroupMock.getCurrentNetworkUsage == 60)
  }
}
