package example

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object FutureEitherExample {

  import DivideError._

  def divideAsyncEither(num: Int, denom: Int): Future[Either[DivideError, Int]] = Future {
    if (denom == 0) Left(ZeroDivision)
    else if (num % denom != 0) Left(Indivisible(num, denom))
    else Right(num / denom)
  }
  def divideBy2AsyncEither(num: Int): Future[Either[DivideError, Int]] = divideAsyncEither(num, 2)

}
