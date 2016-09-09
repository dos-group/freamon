package de.tuberlin.cit.freamon.monitor.utils

import java.io.{File, InputStream}

class FollowingInputStream(val file: File, val waitForNewInput: () => Unit) extends InputStream{

  import java.io.FileInputStream
  private val underlying = new FileInputStream(file)

  def read: Int = handle(underlying.read)

  override def read(b: Array[Byte]): Int = read(b, 0, b.length)

  override def read(b: Array[Byte], off: Int, len: Int): Int = handle(underlying.read(b, off, len))

  override def close(): Unit = underlying.close

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
