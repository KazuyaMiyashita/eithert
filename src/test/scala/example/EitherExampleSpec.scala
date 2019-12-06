package example

import org.scalatest._
import org.scalatest.concurrent.ScalaFutures

class EitherExampleSpec extends WordSpec with Matchers {

  import EitherExample._
  import DivideError._

  "divideBy2Either" should {

    "do once" in {
      val num = 12
      val result = Right(6)
  
      divideBy2Either(num) shouldEqual result
    }

    "do twice (using flatMap)" in {
      val num = 12
      val result = Right(3)
  
      val composed: Either[DivideError, Int] = divideBy2Either(num).flatMap { res1 =>
        divideBy2Either(res1)
      }
      composed shouldEqual result
    }

    "do three times (using flatMap)" in {
      val num = 12
      val result = Left(Indivisible(3, 2))
  
      val composed: Either[DivideError, Int] = divideBy2Either(num).flatMap { res1 =>
        divideBy2Either(res1).flatMap { res2 =>
          divideBy2Either(res2)
        }
      }
      composed shouldEqual result
    }

    "do twice (using for expression)" in {
      val num = 12
      val result = Right(3)
  
      val composed: Either[DivideError, Int] = for {
        res1 <- divideBy2Either(num)
        res2 <- divideBy2Either(res1)
      } yield res2
  
      composed shouldEqual result
    }

    "do three times (using for expression)" in {
      val num = 12
      val result = Left(Indivisible(3, 2))
  
      val composed: Either[DivideError, Int] = for {
        res1 <- divideBy2Either(num)
        res2 <- divideBy2Either(res1)
        res3 <- divideBy2Either(res2)
      } yield res3
  
      composed shouldEqual result
    }

  }

  "divideEither" should {

    "do once (divisible)" in {
      val num = 12
      val denom = 2
      val result = Right(6)
  
      divideEither(num, denom) shouldEqual result
    }

    "do once (indivisible)" in {
      val num = 12
      val denom = 5
      val result = Left(Indivisible(12, 5))
  
      divideEither(num, denom) shouldEqual result
    }

    "do once (zero dividision)" in {
      val num = 12
      val denom = 0
      val result = Left(ZeroDivision)
  
      divideEither(num, denom) shouldEqual result
    }

  }

}
