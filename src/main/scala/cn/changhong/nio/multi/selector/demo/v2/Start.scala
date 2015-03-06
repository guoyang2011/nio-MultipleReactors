package cn.changhong.nio.multi.selector.demo.v2

import java.net.InetSocketAddress
import java.nio.channels.{ SelectionKey, ServerSocketChannel, Selector}


/**
 * Created by yangguo on 15-3-5.
 */


object Start {

  def main(args:Array[String]): Unit ={
    val mainSelector=Selector.open()
    val serverChannel=ServerSocketChannel.open()
    serverChannel.bind(new InetSocketAddress(10003))
    serverChannel.configureBlocking(false)
    serverChannel.register(mainSelector,SelectionKey.OP_ACCEPT)

    val subSelectors=(1 to 4).map(index=>new SubNioSelector(Selector.open())).toArray
    val subCircleSelectorNodes=ConsistentHash(subSelectors)
    val reactorContext=ReactorContext(mainSelector,subCircleSelectorNodes)
    val reactorAction=new MainNioSelector(reactorContext)

    doListener(reactorAction.doAccept)//ServerSocketChannel do accept remote channel event
    subSelectors.foreach(s=>doListener(s.doRead))//Sub Reactor Pool Do Read and Write Channel Network I/O

  }
  def doListener[L](listenerHandler: =>Unit): Unit ={
    new Thread(new Runnable {
      override def run(): Unit = {
        while(true){
          listenerHandler
        }
      }
    }).start()
  }
}
