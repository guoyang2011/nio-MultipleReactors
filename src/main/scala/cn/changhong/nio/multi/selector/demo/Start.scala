package cn.changhong.nio.multi.selector.demo

import java.net.InetSocketAddress
import java.nio.channels.ServerSocketChannel

/**
 * Created by yangguo on 15-3-3.
 */
object Start {
  def main(args:Array[String]): Unit ={
    val serverSocket=ServerSocketChannel.open()
    serverSocket.bind(new InetSocketAddress(10002))
    val reactors=new MultiReactor(serverSocket)
    doSingleThreadJob(reactors.doAccept)
    doSingleThreadJob(reactors.doRead)
    doSingleThreadJob(reactors.doBroadcastServerTime)
  }
  def doSingleThreadJob(fn:()=>Int)={
    new Thread(new Runnable {
      override def run(): Unit = {
        while(true){
          fn()
        }
      }
    }).start()
  }
}
