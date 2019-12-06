package mycats

import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import cats.data.EitherT
import cats.implicits._

class CatsEitherTSpec extends WordSpec with Matchers with ScalaFutures {

  "EitherT of cats" should {

    "EitherT[Future, A, B]" in {

      import example.DivideError
      import example.DivideError.Indivisible
      import example.FutureEitherExample.divideAsyncEither

      val fe: EitherT[Future, DivideError, Int] = for {
        r1 <- EitherT(divideAsyncEither(12, 2))
        r2 <- EitherT(divideAsyncEither(r1, 2))
        r3 <- EitherT(divideAsyncEither(r2, 2))
      } yield r3
      fe.value.futureValue shouldEqual Left(Indivisible(3, 2))

    }

  }

}
