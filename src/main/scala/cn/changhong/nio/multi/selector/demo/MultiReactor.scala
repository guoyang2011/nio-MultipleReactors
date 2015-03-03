package cn.changhong.nio.multi.selector.demo

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.channels.{SocketChannel, SelectionKey, Selector, ServerSocketChannel}
import java.nio.charset.Charset
import java.util.Date
import java.util.concurrent.{Executors, ConcurrentHashMap}

/**
 * Created by yangguo on 15-3-3.
 */

class MultiReactor(serverSocket:ServerSocketChannel) {
  val readSelector=Selector.open()
  val acceptSelector=Selector.open()
  val interruptTime=20000
  lazy val clients=new ConcurrentHashMap[Long,SocketChannel]()
  lazy val workerPool=Executors.newFixedThreadPool(4)
  serverSocket.configureBlocking(false)
  serverSocket.register(acceptSelector,SelectionKey.OP_ACCEPT)


  def doBroadcastServerTime():Int={
    Thread.sleep(interruptTime-1000)
    import scala.collection.JavaConverters._
    val keys=readSelector.keys().asScala
    keys.foreach{key=>
      val channel=key.channel().asInstanceOf[SocketChannel]
      if(channel.isOpen&&channel.isConnected){
        try {
          channel.write(ByteBuffer.wrap(s"Server Time ${new Date().getTime}".getBytes(Charset.forName("utf8"))))
        }catch{
          case ex:Throwable=>unsafeClose(key)
        }
      }else{
        unsafeClose(key)
      }
    }
    0
  }
  def doRead(): Int ={
    val res=if(readSelector.select(interruptTime-10000)>0) {
      var validCount=0
      val keys = readSelector.selectedKeys()
      val it=keys.iterator()
      while(it.hasNext) {
        val key=it.next()//.asInstanceOf[SocketChannel]
        workerPool.submit(new MessageHandler(key))
        validCount+=1
      }
      keys.clear()
      validCount
    }else 0
    println("read channel count="+res)
    res
  }
  private[this] class MessageHandler(key:SelectionKey) extends Runnable{
    override def run(): Unit = decodeMsg(key)
  }
  private[this] def decodeMsg(key:SelectionKey): Unit = {
   if(key.isValid&&key.isReadable) {
      val socket = key.channel().asInstanceOf[SocketChannel]
      val buffer = ByteBuffer.allocate(1024)
      val temp = new ByteArrayOutputStream()

      var isOver = false
      while (!isOver) {
        val count = socket.read(buffer)
        println(s"currentThread=${Thread.currentThread().getName} ,read count=$count")
        if (count <= 0) {
          if (count < 0) {
            unsafeClose(key)
          }
          isOver = true
        }
        temp.write(buffer.array(), 0, count)
      }
      println(new String(temp.toByteArray, "utf8"))
    }
  }

  private[this] def unsafeClose(selectKey:SelectionKey)= {
    try {
      selectKey.channel().close()
    } catch {
      case ex: Throwable => {
        ex.printStackTrace()
      }
    }finally{
      try{
        selectKey.cancel()
      }catch{
        case ex:Throwable=>
      }
    }

  }
  private[this] def registerRead(channel:SocketChannel)={
    channel.configureBlocking(false)
//    channel.register(writeSelector,SelectionKey.OP_WRITE)
    channel.register(readSelector,SelectionKey.OP_READ)
  }
  def doAccept():Int= {
    val res=if (acceptSelector.select(interruptTime) > 0) {
      var validCount = 0
      val keys = acceptSelector.selectedKeys()
      val it = keys.iterator()
      while (it.hasNext) {
        val key = it.next()
        if (key.isValid && key.isAcceptable) {
          val server = key.channel().asInstanceOf[ServerSocketChannel]
          val channel = server.accept()
          registerRead(channel)
          validCount += 1
        }
      }
      keys.clear()
      validCount
    } else {
      0
    }
    println("New Connection Channel Count="+res)
    res
  }
  /**
  def doWrite():Int={
    val res=if(writeSelector.select(interruptTime)>0){
      var validCount=0
      val keys=writeSelector.selectedKeys()
      val it=keys.iterator()
      while (it.hasNext) {
        val key = it.next()
        if (key.isValid && key.isWritable) {
          val channel = key.channel().asInstanceOf[SocketChannel]
          validCount += 1
        }
      }
      keys.clear()
      validCount
    }else{
      0
    }
    println("Ready Write Channel Count="+res)
    res
  }
  */
}
