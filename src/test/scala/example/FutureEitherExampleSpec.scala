package example

import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class FutureEitherExampleSpec extends WordSpec with Matchers with ScalaFutures {

  import FutureEitherExample._
  import DivideError._

  "divideBy2AsyncEither" should {

    "do once" in {
      val num = 12
      val result = Right(6)
  
      divideBy2AsyncEither(num).futureValue shouldEqual result
    }

    "do twice (using flatMap)" in {
      val num = 12
      val result = Right(3)
  
      val composed: Future[Either[DivideError, Int]] = divideBy2AsyncEither(num).flatMap {
        case e@Left(_) => Future.successful(e)
        case Right(res1) => divideBy2AsyncEither(res1)
      }
      composed.futureValue shouldEqual result
    }

    "do three times (using flatMap)" in {
      val num = 12
      val result = Left(Indivisible(3, 2))
  
      val composed: Future[Either[DivideError, Int]] = divideBy2AsyncEither(num).flatMap {
        case v@Left(_) => Future.successful(v)
        case Right(res1) => {
          divideBy2AsyncEither(res1).flatMap {
            case v@Left(_) => Future.successful(v)
            case Right(res2) => divideBy2AsyncEither(res2)
          }
        }
      }
      composed.futureValue shouldEqual result
    }

    "do twice (using for expression)" in {
      val num = 12
      val result = Right(3)
  
      val composed: Future[Either[DivideError, Int]]= for {
        res1 <- divideBy2AsyncEither(num)
        res2 <- res1 match {
          case v@Left(_) => Future.successful(v)
          case Right(value) => divideBy2AsyncEither(value)
        }
      } yield res2
  
      composed.futureValue shouldEqual result
    }

    "do three times (using for expression)" in {
      val num = 12
      val result = Left(Indivisible(3, 2))
  
      val composed: Future[Either[DivideError, Int]]= for {
        res1 <- divideBy2AsyncEither(num)
        res2 <- res1 match {
          case v@Left(_) => Future.successful(v)
          case Right(value) => divideBy2AsyncEither(value)
        }
        res3 <- res2 match {
          case v@Left(_) => Future.successful(v)
          case Right(value) => divideBy2AsyncEither(value)
        }
      } yield res3
  
      composed.futureValue shouldEqual result
    }

  }

  "divideAsyncEither" should {

    "do once (divisible)" in {
      val num = 12
      val denom = 2
      val result = Right(6)
  
      divideAsyncEither(num, denom).futureValue shouldEqual result
    }

    "do once (indivisible)" in {
      val num = 12
      val denom = 5
      val result = Left(Indivisible(12, 5))
  
      divideAsyncEither(num, denom).futureValue shouldEqual result
    }

    "do once (zero dividision)" in {
      val num = 12
      val denom = 0
      val result = Left(ZeroDivision)
  
      divideAsyncEither(num, denom).futureValue shouldEqual result
    }

  }

}
