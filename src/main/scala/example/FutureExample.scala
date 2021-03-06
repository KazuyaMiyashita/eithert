package example

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object FutureExample {

  def divideAsync(num: Int, denom: Int): Future[Int] = Future {
    if (denom == 0) throw new IllegalArgumentException("denom must not be 0")
    if (num % denom != 0) throw new Exception(s"$num is not divisible by $denom")
    num / denom
  }

  def divideBy2Async(num: Int): Future[Int] = divideAsync(num, 2)

}
