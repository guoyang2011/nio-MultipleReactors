package cn.changhong.nio.multi.selector.demo

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.channels.{SocketChannel, SelectionKey, Selector, ServerSocketChannel}
import java.nio.charset.Charset
import java.util.concurrent.{Executors, ConcurrentHashMap}

import cn.changhong.nio.multi.selector.demo.v2.ConsistentHash

/**
 * Created by yangguo on 15-3-3.
 */
@deprecated("use v2 model")
class MultiReactor(serverSocket:ServerSocketChannel,subSelectorCount:Int=4) {
  val readSelectors=(0 to subSelectorCount).toArray.map(index=>Selector.open())
  val subSelectors=ConsistentHash[Selector](readSelectors)
  val acceptSelector=Selector.open()
  val interruptTime=20000
  lazy val clients=new ConcurrentHashMap[Long,SocketChannel]()
  lazy val workerPool=Executors.newFixedThreadPool(4)
  serverSocket.configureBlocking(false)
  serverSocket.register(acceptSelector,SelectionKey.OP_ACCEPT)
  def doBroadcastServerTime(index:Int):Int={
    Thread.sleep(interruptTime-1000)
    import scala.collection.JavaConverters._
    val keys=readSelectors.flatMap(s=>s.keys().asScala)
    val response=s"Server Time ${System.currentTimeMillis()}".getBytes(Charset.forName("utf8"))
    keys.foreach{key=>
      val channel=key.channel().asInstanceOf[SocketChannel]
      if(channel.isOpen&&channel.isConnected){
        try {
          channel.write(ByteBuffer.wrap(response))
        }catch{
          case ex:Throwable=>unsafeClose(key)
        }
      }else{
        unsafeClose(key)
      }
    }
    0
  }
  def doRead(index:Int): Int ={
    val readSelector=readSelectors(index)
    val res=if(readSelector.select(interruptTime-10000)>0) {
      var validCount=0
      val keys = readSelector.selectedKeys()
      val it=keys.iterator()
      while(it.hasNext) {
        val key=it.next()
        key.interestOps(key.interestOps() & ~SelectionKey.OP_READ)
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
    override def run(): Unit = readMsg(key)
  }
  private[this] def readMsg(key:SelectionKey): Unit = {
   if(key.isValid&&key.isReadable) {
     val socket = key.channel().asInstanceOf[SocketChannel]
     val buffer = ByteBuffer.allocate(1024)
     val temp = new ByteArrayOutputStream()
     val slickedData=ByteBuffer.allocate(1024)
     var isOver = false
     var isClosed=false
     while (!isOver) {
       buffer.clear()
       val count = socket.read(buffer)
       buffer.flip()
       while(buffer.hasRemaining){
         val b=buffer.get()
         if(b=='#'.toByte){
           slickedData.flip()
           println("包数据package data="+new String(slickedData.array()))
           slickedData.clear()
         }else {
           slickedData.put(b)
         }
       }
       if (count <= 0) {
         if (count < 0) {
           isClosed=true
           unsafeClose(key)
         }
         isOver = true
       }
       if(count>0) temp.write(buffer.array(), 0, count)
     }
     if(!isClosed) key.interestOps(key.interestOps() | SelectionKey.OP_READ)
     println(s"currentThread=${Thread.currentThread().getName} ,read count=${temp.size()}")
     println("read message=[" + new String(temp.toByteArray, "utf8") + "]")
     temp.close()
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
//    val index:Int=(System.currentTimeMillis()%4).toInt//
    subSelectors.get(channel) match { //consistent hash
      case Some(selector)=>channel.register (selector, SelectionKey.OP_READ)
      case None=>println("register error")
    }
  }
  def doAccept(index:Int):Int= {
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
    println("New Connection Channel Count="+res+",accept selector count="+acceptSelector.keys().size())
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
