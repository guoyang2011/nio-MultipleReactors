package cn.changhong.nio.multi.selector.demo.v2

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.channels.{SocketChannel, SelectionKey, ServerSocketChannel, Selector}

import v2.TwitterAsyncExecutorProvider

/**
 * Created by yangguo on 15-3-6.
 */
class SubNioSelector(val selector:Selector) extends AbstractReactorSelector{
  def doRead(): Unit =  {
    try {
      println(s"do read selector,${selector.toString}")
      if (selector.select() > 0) {
        val readyKeys = selector.selectedKeys()
        val it = readyKeys.iterator()
        while (it.hasNext) {
          val key = it.next()
          key.interestOps(key.interestOps() & ~SelectionKey.OP_READ)
//          readMsg(key)
          /*

           */
          TwitterAsyncExecutorProvider(key, readMsg)
        }
        readyKeys.clear()
      }
    } catch {
      case ex: Throwable => ex.printStackTrace()
    }
  }

  def registerRead(channel: SocketChannel) =  {
    channel.configureBlocking(false)
    println("begin register...")
    /*
    in other thread ,selector while doing  blocking selection operation,
    publicKeys is locked,add Register operation need publicKeys lock.
    this operation will wake up blocking selection operation release publicKeys lock.
    Selector Impl sun.nio.ch
    {
        // The set of keys with data ready for an operation
        protected Set<SelectionKey> selectedKeys;

        // The set of keys registered with this Selector
        protected HashSet<SelectionKey> keys;

        // Public views of the key sets
        private Set<SelectionKey> publicKeys;             // Immutable
        private Set<SelectionKey> publicSelectedKeys;     // Removal allowed, but not addition
        protected final SelectionKey register(AbstractSelectableChannel ch,
                                          int ops,
                                          Object attachment)
        {
            if (!(ch instanceof SelChImpl))
                throw new IllegalSelectorException();
            SelectionKeyImpl k = new SelectionKeyImpl((SelChImpl)ch, this);
            k.attach(attachment);
            synchronized (publicKeys) {//key set lock so need this unlock in selection operation
                implRegister(k);
            }
            k.interestOps(ops);
            return k;
        }
        private int lockAndDoSelect(long timeout) throws IOException {
          synchronized (this) {
              if (!isOpen())
                  throw new ClosedSelectorException();
              synchronized (publicKeys) {
                  synchronized (publicSelectedKeys) {
                      return doSelect(timeout);
                  }
              }
          }
       }
    }

    */
//    lock.synchronized
    {//Because Selector Object Set Key is Not Thread Safe,And
      selector.wakeup()//
      channel.register(selector, SelectionKey.OP_READ)
    }
    println("end register")
  }
  private[this] def readMsg(key: SelectionKey): Unit = {
    try {
      if (key.isValid && key.isReadable) {
        val socket = key.channel().asInstanceOf[SocketChannel]
        val buffer = ByteBuffer.allocate(1024)
        val temp = new ByteArrayOutputStream()
        val slickedData = ByteBuffer.allocate(1024)
        var isOver = false
        var isClosed = false
        while (!isOver) {
          buffer.clear()
          val count = socket.read(buffer)
          buffer.flip()
          while (buffer.hasRemaining) {
            val b = buffer.get()
            if (b == '#'.toByte) {
              slickedData.flip()
              println("包数据package data=" + new String(slickedData.array()))
              slickedData.clear()
            } else {
              slickedData.put(b)
            }
          }
          if (count <= 0) {
            if (count < 0) {
              isClosed = true
              unsafeClose(key)
            }
            isOver = true
          } else temp.write(buffer.array(), 0, count)
        }
        if (!isClosed) {
          key.selector().wakeup()
          key.interestOps(key.interestOps() | SelectionKey.OP_READ)
        }
        println(s"currentThread=${Thread.currentThread().getName} ,read count=${temp.size()}")
        println("read message=[" + new String(temp.toByteArray, "utf8") + "]")
        temp.close()
      }
    }catch{
      case ex:Throwable=>ex.printStackTrace()
    }
  }
}