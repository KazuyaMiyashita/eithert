package mylib

import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class FutureEitherSpec extends WordSpec with Matchers with ScalaFutures {

  "map" should {

    "Successful(Right(_))" in {
      val fe: FutureEither[String, Int] = FutureEither(Future(Right(42)))
      val fe2: FutureEither[String, Int] = fe.map(_ + 1)
      val fe3: FutureEither[String, Int] = fe2.map(_ + 1)
      
      val a: Future[Either[String, Int]] = fe3.value
      a.futureValue shouldEqual Right(44)
    }

    "Successful(Left(_))" in {
      val fe: FutureEither[String, Int] = FutureEither(Future(Left("left")))
      val fe2: FutureEither[String, Int] = fe.map(_ + 1)

      val a: Future[Either[String, Int]] = fe2.value
      a.futureValue shouldEqual Left("left")
    }

    "Failure(_)" in {
      val fe: FutureEither[String, Int] = FutureEither(Future(throw new Exception("err")))
      val fe2: FutureEither[String, Int] = fe.map(_ + 1)

      val a: Future[Either[String, Int]] = fe2.value
      a.failed.futureValue.isInstanceOf[Exception]
    }

  }

  "flatMap" should {

    "Successful(Right(_))" in {
      val fe: FutureEither[String, Int] = FutureEither(Future(Right(42)))
      val fe2: FutureEither[String, Int] = fe.flatMap(v => FutureEither(Future(Right(v + 1))))
      val fe3: FutureEither[String, Int] = fe2.flatMap(v => FutureEither(Future(Right(v + 1))))

      val a: Future[Either[String, Int]] = fe3.value
      a.futureValue shouldEqual Right(44)
    }

    "Successful(Left(_))" in {
      val fe: FutureEither[String, Int] = FutureEither(Future(Left("left")))
      val fe2: FutureEither[String, Int] = fe.flatMap(v => FutureEither(Future(Right(v + 1))))

      val a: Future[Either[String, Int]] = fe2.value
      a.futureValue shouldEqual Left("left")
    }

    "Failure(_)" in {
      val fe: FutureEither[String, Int] = FutureEither(Future(throw new Exception("err")))
      val fe2: FutureEither[String, Int] = fe.flatMap(v => FutureEither(Future(Right(v + 1))))

      val a: Future[Either[String, Int]] = fe2.value
      a.failed.futureValue.isInstanceOf[Exception]
    }

  }

  "for expression example" should {

    import example.DivideError
    import example.DivideError.Indivisible
    import example.FutureEitherExample.divideAsyncEither

    "ex1" in {
      val fe: FutureEither[DivideError, Int] = for {
        r1 <- FutureEither(divideAsyncEither(12, 2))
        r2 <- FutureEither(divideAsyncEither(r1, 2))
        r3 <- FutureEither(divideAsyncEither(r2, 2))
      } yield r3
      fe.value.futureValue shouldEqual Left(Indivisible(3, 2))
    }

  }
  
}
