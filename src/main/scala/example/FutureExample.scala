package example

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object FutureExample {

  /**
   * 割り切れない場合は切り捨て
   * ゼロ割りは java.lang.ArithmeticException
   */
  def divideAsync(num: Int, denom: Int): Future[Int] = Future(num / denom)

  def divideBy2Async(num: Int): Future[Int] = divideAsync(num, 2)

}
