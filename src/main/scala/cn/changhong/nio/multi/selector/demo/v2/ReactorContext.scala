package cn.changhong.nio.multi.selector.demo.v2

import java.nio.channels.Selector


/**
 * Created by yangguo on 15-3-5.
 */
case class ReactorContext(mainSelector:Selector,subSelectors:ConsistentHash[SubNioSelector])
