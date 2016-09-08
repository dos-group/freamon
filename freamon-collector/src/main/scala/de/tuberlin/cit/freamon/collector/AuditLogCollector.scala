package de.tuberlin.cit.freamon.collector

import java.io.{File, InputStream}

object AuditLogCollector {

  def start(args: String): Unit = {
    import java.io.{File, BufferedReader, InputStreamReader}
    /*if (args.length != 1) {
      println("Parameters: <path/to/hdfs-audit.log> <seconds to run>")
      return
    }*/

    val logFile = new File(args(0))
    val br = new BufferedReader(new InputStreamReader(follow(logFile)))

    def read: Unit = {
      val l = br.readLine
      if(l != null){
        println(l)
        read
      }
    }
    read
  }
  def follow(file: File): InputStream = {
      val maxRetries = 3
      val waitToOpen = 1000
      val waitBetweenReads = 100

      def sleep(msec: Long) = () => Thread.sleep(msec)

      follow(file, maxRetries, sleep(waitToOpen), sleep(waitBetweenReads))
  }

  def follow(file: File, openTries: Int, openSleep: () => Unit, rereadSleep: () => Unit): InputStream = {
      import java.io.SequenceInputStream

      val e = new java.util.Enumeration[InputStream]() {
        def nextElement = new FollowingInputStream(file, rereadSleep)
        def hasMoreElements = testExists(file, openTries, openSleep)
      }

      new SequenceInputStream(e)
   }

  def testExists(file: File, tries: Int, sleep: () => Unit): Boolean = {
      def tryExists(n: Int): Boolean =
        if (file.exists) true
        else if (n > tries) false
        else {
          sleep()
          tryExists(n+1)
        }
      tryExists(1)
  }

}

class FollowingInputStream(val file: File, val waitForNewInput: () => Unit) extends InputStream{

  import java.io.FileInputStream
  private val underlying = new FileInputStream(file)

  def read: Int = handle(underlying.read)

  override def read(b: Array[Byte]): Int = read(b, 0, b.length)

  override def read(b: Array[Byte], off: Int, len: Int): Int = handle(underlying.read(b, off, len))

  override def close = underlying.close

  protected def rotated_? = try { underlying.getChannel.position() > file.length }

  protected def closed_? = !underlying.getChannel.isOpen

  protected def handle(read: => Int): Int = read match {
    case -1 if rotated_? || closed_? => -1
    case -1 =>
      waitForNewInput()
      handle(read)
    case i => i
  }

  require(file != null)
  assume(file.exists)

}
