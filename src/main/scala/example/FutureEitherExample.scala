package example

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object FutureEitherExample {

  import EitherExample.divideEither
  import DivideError._

  def divideAsyncEither(num: Int, denom: Int): Future[Either[DivideError, Int]] = {
    Future(divideEither(num, denom))
  }
  def divideBy2AsyncEither(num: Int): Future[Either[DivideError, Int]] = divideAsyncEither(num, 2)

}
