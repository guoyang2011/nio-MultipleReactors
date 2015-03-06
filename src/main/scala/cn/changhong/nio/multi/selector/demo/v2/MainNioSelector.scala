package cn.changhong.nio.multi.selector.demo.v2

import java.nio.channels.{ServerSocketChannel, Selector}

/**
 * Created by yangguo on 15-3-6.
 */
class MainNioSelector(val context:ReactorContext) extends AbstractReactorSelector{
  val selector:Selector=context.mainSelector
  def doAccept() :Unit= {
    try {
      if (selector.select() > 0) {
        val keys = selector.selectedKeys()
        val it = keys.iterator()
        println(keys.size() + "accept socket keys")
        while (it.hasNext) {
          val key = it.next()
          if (key.isValid && key.isAcceptable) {
            val server = key.channel().asInstanceOf[ServerSocketChannel]
            val remoteChannel = server.accept()
            context.subSelectors.get(key).foreach(_.registerRead(remoteChannel))
          }
        }
        keys.clear()
      }
    }catch{
      case ex:Throwable=>ex.printStackTrace()
    }
  }
}
