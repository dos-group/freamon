package de.tuberlin.cit.freamon.collector

import java.io.{BufferedReader, File, InputStream, InputStreamReader}
import java.text.SimpleDateFormat
import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}

import de.tuberlin.cit.freamon.api.AuditLogEntry

object NewAuditLogCollector{


  class Producer[T](path: String, queue: BlockingQueue) extends Runnable {
    var br: BufferedReader = null

    def run(): Unit = {
      val logFile = new File(path)
      br = new BufferedReader(new InputStreamReader(follow(logFile)))
      read()

    }

    def read(): Unit = {
      var l: String = null
      try {
        l = br.readLine()
      }
      catch {
        case e: Exception =>
          println("Caught an exception: " + e.getCause)
          e.getStackTrace
      }
      println("Contents of l: " + l)
      if (l != null) {
        val e: AuditLogEntry = processEntry(l)
        println("An AuditLogEntry object created with date: " + e.date)
      }
      read()
    }

    def processEntry(entry: String): AuditLogEntry = {
      val auditLog: AuditLogEntry = new AuditLogEntry

      val sdf: SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS")

      val date: String = entry.substring(0, 23)
      auditLog.date = sdf.parse(date).getTime
      val report: String = entry.substring(49)
      val splitReport: Array[String] = report.split("\t")
      val allowed = splitReport(0).substring(8)
      if (allowed == "true")
        auditLog.allowed = true
      else if (allowed == "false")
        auditLog.allowed = false
      auditLog.ugi = splitReport(1).substring(4)
      auditLog.ip = splitReport(2).substring(3)
      auditLog.cmd = splitReport(3).substring(4)
      auditLog.src = splitReport(4).substring(4)
      auditLog.dst = splitReport(5).substring(4)
      auditLog.perm = splitReport(6).substring(5)
      auditLog.proto = splitReport(7).substring(6)

      auditLog
    }

    def follow(file: File): InputStream = {
      val maxRetries = 3
      val waitToOpen = 1000
      val waitBetweenReads = 100

      def sleep(msec: Long) = () => Thread.sleep(msec)

      follow2(file, maxRetries, sleep(waitToOpen), sleep(waitBetweenReads))
    }

    def follow2(file: File, openTries: Int, openSleep: () => Unit, rereadSleep: () => Unit): InputStream = {
      import java.io.SequenceInputStream

      val e = new java.util.Enumeration[InputStream] {
        def nextElement = new FollowingInputStream2(file, rereadSleep)

        def hasMoreElements = testExists(file, openTries, openSleep)
      }
      new SequenceInputStream(e)
    }

    def testExists(file: File, tries: Int, sleep: () => Unit): Boolean = {
      def tryExists(n: Int): Boolean =
        if (file.exists()) true
        else if (n > tries) false
        else {
          sleep()
          tryExists(n + 1)
        }
      tryExists(1)
    }

  }

  abstract class Consumer[AuditLogEntry](queue: BlockingQueue[AuditLogEntry]) extends Runnable {
    def run(): Unit = {
      while (true) {
        val item = queue.take()
        consume(item)
      }
    }

    def consume(x: AuditLogEntry)

    def checkIfEmpty()
  }

  val queue = new LinkedBlockingQueue[AuditLogEntry]()

  def startProducer(path: String): Unit ={
    val producer = new Producer[AuditLogEntry](path, queue)
    new Thread(producer).start()
  }

  var consumer: ForwardingConsumer = null
  var consumerThread: Thread = null

  def startConsumer(): Unit = {
    consumer = new ForwardingConsumer(queue)
    consumerThread = new Thread(consumer)
    consumerThread.start()
  }



  class ForwardingConsumer(queue: BlockingQueue[AuditLogEntry]) extends Consumer[AuditLogEntry](queue){
    def consume(entry: AuditLogEntry): AuditLogEntry = {
      println("Received an AuditLogEntry object forwarding...")
      entry
    }

    def checkIfEmpty(): Boolean = {
      queue.isEmpty
    }
  }

}

import java.io.{File, InputStream}

class FollowingInputStream2(val file: File, val waitForNewInput: () => Unit) extends InputStream{
  import java.io.FileInputStream



  private val underlying = new FileInputStream(file)

  def read: Int = handle(underlying.read())

  override def read(b: Array[Byte]): Int = read(b, 0, b.length)

  override def read(b: Array[Byte], off: Int, len: Int): Int = handle(underlying.read(b, off, len))

  override def close(): Unit = underlying.close()

  protected def rotated_? = try { underlying.getChannel.position() > file.length() }

  protected def closed_? = !underlying.getChannel.isOpen

  protected def handle(read: => Int): Int = read match {
    case -1 if rotated_? || closed_? => -1
    case -1 =>
      waitForNewInput()
      handle(read)
    case i => 1
  }
  require(file != null)
  assume(file.exists())
}

