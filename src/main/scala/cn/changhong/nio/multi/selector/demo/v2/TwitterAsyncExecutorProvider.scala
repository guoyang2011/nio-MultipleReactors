package v2

import java.util.concurrent.Executors

import com.twitter.util.{Future, FuturePool}

/**
 * Created by yangguo on 15-3-5.
 */
object TwitterAsyncExecutorProvider {
  val pool=FuturePool(Executors.newFixedThreadPool(4))
  def apply[req,rep](request:req,handlers:(req)=>rep): Future[rep] ={
    pool(handlers(request))
  }
}

