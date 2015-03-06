package cn.changhong.nio.multi.selector.demo.v2

import java.nio.channels.SelectionKey

/**
 * Created by yangguo on 15-3-6.
 */
abstract class AbstractReactorSelector {
  def unsafeClose(selectKey: SelectionKey): Unit = {
    try {
      selectKey.channel().close()
    } catch {
      case ex: Throwable => {
        ex.printStackTrace()
      }
    } finally {
      try {
        selectKey.cancel()
      } catch {
        case ex: Throwable =>
      }
    }
  }
}
