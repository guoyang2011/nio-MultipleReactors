package cn.changhong.nio.multi.selector.demo.v2

import java.util.{ArrayList, TreeMap}

import scala.collection.JavaConverters._


/**
 * Created by yangguo on 15-3-4.
 */
class ConsistentHash[T](numberOfReplicas:Int,circle:TreeMap[Long,T],hashFunc:(Object)=>Long) {
  private val list=new ArrayList[T]()
  def add(node:T): Unit ={
    list.add(node)
    (0 to numberOfReplicas).foreach{index=>
      circle.put(hashFunc(node.toString+index),node)
    }
  }
  def remove(node:T): Unit ={
    list.remove(node)
    (0 to numberOfReplicas).foreach{index=>
      circle.remove(hashFunc(node.toString+index))
    }
  }
  def get(key:Object):Option[T]={
    if(circle.isEmpty) None
    else {
      val hash= {
        var tempHash=hashFunc(key)
        println(tempHash+"")
        if (!circle.containsKey(tempHash)) {
          val tailMap = circle.tailMap(tempHash)
          tempHash= {
            if (tailMap.isEmpty) circle.firstKey()
            else tailMap.firstKey()
          }
        }
        tempHash
      }
      Some(circle.get(hash))
    }
  }
  def getSize()={
    circle.size()
  }
  def printNodes()={
    circle.entrySet().asScala.foreach(kv=>println(kv.getKey+"->"+kv.getValue))
  }
  def getAllNode=list
}

object ConsistentHash {
  def fnv1HashFunc(key:Object):Long={
    val data=key.toString
    val p=16777619
    var hash=2166136261L.toInt
    hash=data.foldLeft(hash)((h,i)=>(h ^ i)*p)
    hash+=hash<<13
    hash^=hash>>7
    hash+=hash<<3
    hash^=hash>>17
    hash+=hash<<5
    hash
  }
  def apply[T](nodes: Array[T], numberOfNodes:Int=167,hashFunc:(Object)=>Long=fnv1HashFunc):ConsistentHash[T]={
    val hashImpl=new ConsistentHash(numberOfNodes,new TreeMap[Long,T](),hashFunc)
    nodes.foreach(hashImpl.add(_))
    hashImpl
  }
}


