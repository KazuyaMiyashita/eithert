package myscalaz

import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scalaz.EitherT
import scalaz.std.scalaFuture._

class CatsEitherTSpec extends WordSpec with Matchers with ScalaFutures {

  "EitherT of cats" should {

    "EitherT[Future, A, B]" in {

      import example.DivideError
      import example.DivideError.Indivisible
      import example.FutureEitherExample.divideAsyncEither

      val fe: EitherT[Future, DivideError, Int] = for {
        r1 <- EitherT.fromEither(divideAsyncEither(12, 2))
        r2 <- EitherT.fromEither(divideAsyncEither(r1, 2))
        r3 <- EitherT.fromEither(divideAsyncEither(r2, 2))
      } yield r3
      fe.run.map(_.toEither).futureValue shouldEqual Left(Indivisible(3, 2))

    }

  }

}
