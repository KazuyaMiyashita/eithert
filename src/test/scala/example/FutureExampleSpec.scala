package example

import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class FutureExampleSpec extends WordSpec with Matchers with ScalaFutures {

  import FutureExample._

  "divideBy2Async" should {

    "do once" in {
      val num = 12
      val result = 6
  
      divideBy2Async(num).futureValue shouldEqual result
    }

    "do twice (using flatMap)" in {
      val num = 12
      val result = 3
  
      val composed: Future[Int] = divideBy2Async(num).flatMap { res1 =>
        divideBy2Async(res1)
      }
      composed.futureValue shouldEqual result
    }

    "do three times (using flatMap)" in {
      val num = 12
 
      val composed: Future[Int] = divideBy2Async(num).flatMap { res1 =>
        divideBy2Async(res1).flatMap { res2 =>
          divideBy2Async(res2)
        }
      }
      composed.failed.futureValue.getMessage shouldEqual "3 is not divisible by 2"
    }

    "do twice (using for expression)" in {
      val num = 12
      val result = 3
  
      val composed: Future[Int] = for {
        res1 <- divideBy2Async(num)
        res2 <- divideBy2Async(res1)
      } yield res2
  
      composed.futureValue shouldEqual result
    }

    "do three times (using for expression)" in {
      val num = 12
  
      val composed: Future[Int] = for {
        res1 <- divideBy2Async(num)
        res2 <- divideBy2Async(res1)
        res3 <- divideBy2Async(res2)
      } yield res3
  
      composed.failed.futureValue.getMessage shouldEqual "3 is not divisible by 2"
    }

  }

  "divideAsync" should {

    "do once (divisible)" in {
      val num = 12
      val denom = 2
      val result = 6
  
      divideAsync(num, denom).futureValue shouldEqual result
    }

    "do once (indivisible)" in {
      val num = 12
      val denom = 5
  
      divideAsync(num, denom).failed.futureValue.getMessage shouldEqual "12 is not divisible by 5"
    }

    "do once (zero dividision)" in {
      val num = 12
      val denom = 0
  
      divideAsync(num, denom).failed.futureValue.isInstanceOf[ArithmeticException]
    }

  }

}
