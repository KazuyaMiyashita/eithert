package example

object EitherExample {

  import DivideError._

  def divideEither(num: Int, denom: Int): Either[DivideError, Int] = {
    if (denom == 0) Left(ZeroDivision)
    else if (num % denom != 0) Left(Indivisible(num, denom))
    else Right(num / denom)
  }
  def divideBy2Either(num: Int): Either[DivideError, Int] = divideEither(num, 2)

}

sealed trait DivideError
object DivideError {
  case class Indivisible(num: Int, denom: Int) extends DivideError
  object ZeroDivision extends DivideError
}
