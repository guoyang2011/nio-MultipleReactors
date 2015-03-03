package cn.changhong.nio.multi.selector.demo

import java.net.InetSocketAddress
import java.nio.channels.ServerSocketChannel

/**
 * Created by yangguo on 15-3-3.
 */
object Start {
  def main(args:Array[String]): Unit ={
    val subReactorCount=4
    val serverSocket=ServerSocketChannel.open()
    serverSocket.bind(new InetSocketAddress(10002))
    val reactors=new MultiReactor(serverSocket,subReactorCount)
    doSingleThreadJob(0,reactors.doAccept)
    (0 to subReactorCount).foreach(doSingleThreadJob(_,reactors.doRead))
    doSingleThreadJob(0,reactors.doBroadcastServerTime)
  }
  def doSingleThreadJob(index:Int,fn:(Int)=>Int)={
    new Thread(new Runnable {
      override def run(): Unit = {
        while(true){
          fn(index)
        }
      }
    }).start()
  }
}
