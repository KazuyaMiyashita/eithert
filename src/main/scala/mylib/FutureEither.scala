package mylib

import scala.concurrent.{Future, ExecutionContext}

case class FutureEither[A, B](value: Future[Either[A, B]]) {

  def map[BB](f: B => BB)(implicit ec: ExecutionContext): FutureEither[A, BB] = {
    FutureEither(value.map(_.map(f))(ec))
  }

  def flatMap[BB](f: B => FutureEither[A, BB])(implicit ec: ExecutionContext): FutureEither[A, BB] = {
    FutureEither(
      value.flatMap {
        case l @ Left(_) => Future.successful(l.asInstanceOf[Either[A, BB]])
        case Right(r) => f(r).value
      }
    )
  }

}
