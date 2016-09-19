package de.tuberlin.cit.freamon.collector

import java.io._
import java.text.SimpleDateFormat
import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}

import de.tuberlin.cit.freamon.api.AuditLogEntry

object NewAuditLogCollector{

  class Producer[T](path: String, queue: BlockingQueue[AuditLogEntry]) extends Runnable {
    println("Producer Class")

    var br: BufferedReader = null
    val logFile = new File(path)
    br = new BufferedReader(new InputStreamReader(follow(logFile)))

    def run(): Unit = {
      println("Producer.run called")

      read()

    }

    def read(): Unit = {
      println("Producer.read called")
      println("Read the line!: "+br.readLine())
      var l: String = br.readLine()
      if(l==null)
        println("l is null")
      else println("l is not null")
      try {
        l = br.readLine()
      }
      catch {
        case e: IOException =>
          println("Caught an exception: " + e.getCause)
          e.getStackTrace
      }
      println("Contents of l: " + l)
      if (l != null) {
        val e: AuditLogEntry = processEntry(l)
        queue.add(e)
        println("An AuditLogEntry object created with date: " + e.date)
      }
      read()
    }

    def processEntry(entry: String): AuditLogEntry = {
      println("Producer.processEntry called")
      val sdf: SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS")

      val date: String = entry.substring(0,23)
      val parsedDate = sdf.parse(date).getTime
      val report = entry.substring(49)
      val splitReport: Array[String] = report.split("\t")
      val allowed = splitReport(0).substring(8)
      var boolAllowed: Boolean = false
      if(allowed=="true")
        boolAllowed = true
      else if (allowed=="false")
        boolAllowed = false
      val ugi = splitReport(1).substring(4)
      val ip = splitReport(2).substring(3)
      val cmd = splitReport(3).substring(4)
      val src =splitReport(4).substring(4)
      val dst = splitReport(5).substring(4)
      val perm = splitReport(6).substring(5)
      val proto = splitReport(7).substring(6)

      val auditLog:AuditLogEntry = new AuditLogEntry(parsedDate, boolAllowed, ugi, ip, cmd, src, dst, perm, proto)
      auditLog
    }

    def follow(file: File): InputStream = {
      println("Producer.follow called")
      val maxRetries = 3
      val waitToOpen = 1000
      val waitBetweenReads = 100

      def sleep(msec: Long) = () => Thread.sleep(msec)

      follow2(file, maxRetries, sleep(waitToOpen), sleep(waitBetweenReads))
    }

    def follow2(file: File, openTries: Int, openSleep: () => Unit, rereadSleep: () => Unit): InputStream = {
      import java.io.SequenceInputStream
      println("Producer.follow2 called")

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

  class Consumer[AuditLogEntry](queue: BlockingQueue[AuditLogEntry]) extends Runnable {
    def run(): Unit = {
      println("abstract Consumer.run called")
      while (true) {
        val item = queue.take()
        consume(item)
      }
    }

    def consume(entry: AuditLogEntry): AuditLogEntry = {
      println("Received an AuditLogEntry object forwarding...")
      entry
    }

    def checkIfEmpty(): Boolean = {
      println("ForwardingConsumer.checkIfEmpty called. queue is Empty: "+queue.isEmpty+", size: "+queue.size())
      queue.isEmpty
    }
  }

  val queue = new LinkedBlockingQueue[AuditLogEntry]()
  var producerThread: Thread = null

  def startProducer(path: String): Unit ={
    val producer = new Producer[AuditLogEntry](path, queue)
    producerThread = new Thread(producer)
    producerThread.start()
  }

  var consumer: Consumer[AuditLogEntry] = null
  var consumerThread: Thread = null

  def startConsumer(): Unit = {
    consumer = new Consumer(queue)
    consumerThread = new Thread(consumer)
    consumerThread.start()
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

