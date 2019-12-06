# はじめてのEitherT

この記事は [Scala Advent Calendar 2019](https://qiita.com/advent-calendar/2019/scala) の7日目の記事です。

* この記事のまとめ: `Future[Either[A, B]]` は CatsやScalazなどを使って `EitherT[Future, A, B]` に変換すると合成が簡単にできる

## これまでのあらすじ



### Either

* `Either` は成功時の値か失敗時の値のどちらかを表すことができる
* 言い換えると、失敗する可能性のある処理の返り値の型は `Either[A, B]` と書ける

```scala
sealed trait DivideError
object DivideError {
  case class Indivisible(num: Int, denom: Int) extends DivideError
  object ZeroDivision extends DivideError
}

def divideEither(num: Int, denom: Int): Either[DivideError, Int] = ???
```

```scala
divideEither(4, 2) // Right(2)
divideEither(4, 3) // Left(Indivisible(4,3))
divideEither(4, 0) // Left(ZeroDivision)
```

* Eitherの結果に対して、もし成功していたらさらに失敗する可能性のある処理を合成することができる

```scala
val res1: Either[DivideError, Int] = divideEither(12, 2) // Right(6)
val res2: Either[DivideError, Int] = res1.flatMap { r => divideEither(r, 2) } // Right(3)
val res3: Either[DivideError, Int] = res2.flatMap { r => divideEither(r, 2) } // Left(Indivisible(3, 2))
```

* 上記のres3はflatMapはfor式でより簡潔に書くことができる

```scala
val res3 = for {
  r1 <- divideEither(12, 2)
  r2 <- divideEither(r1, 2)
  r3 <- divideEither(r2, 2)
} yield r3
```

### Future

* `Future` は非同期処理を簡単に行なうことのできるツール
* `Future` にくるまれた値は、ある時点において利用可能になる可能性のある値を保持する

```scala
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure}

def divideAsync(num: Int, denom: Int): Future[Int] = Future(num / denom)
```

```scala
val f1: Future[Int] = divideAsync(4, 2)
f1.onComplete {
  case Success(value) => println(value)
  case Failure(e) => e.printStackTrace()
}

val f2 = f1.flatMap { r => devideAsync(r, 2) }
f2.onComplete {
  case Success(value) => println(value)
  case Failure(e) => e.printStackTrace()
}
```

```scala
val a = divideAsync(1, 0)
a.onComplete {
  case Success(value) => println(value)
  case Failure(e) => e.printStackTrace()
}

// java.lang.ArithmeticException: / by zero
// a: scala.concurrent.Future[Int] = Future(Failure(java.lang.ArithmeticException: / by zero))
//         at example.FutureExample$.$anonfun$divideAsync$1(FutureExample.scala:12)
//         ...
```

## Future[Either[A, B]] を使う

* EitherもFutureも便利そうなので、同時に使いたい！

```scala
def divideAsyncEither(num: Int, denom: Int): Future[Either[DivideError, Int]]
```

```scala
val a = divideAsyncEither(4, 2)
a.onComplete {
  case Success(value) => value match {
    case Right(num) => println(num)
    case Left(Indivisible(n, d)) => println(s"$n は $d で割り切れません")
    case Left(ZeroDivision) => println("ゼロ割りはできません")
  }
  case Failure(e) => e.printStackTrace()
}
```

* 合成が非常に面倒

```scala
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
```

## Future[Either[A, B]] の合成を簡単にするためのクラスを作る

```scala
case class FutureEither[A, B](value: Future[Either[A, B]]) {
  def map[BB](f: B => BB)(implicit ec: ExecutionContext): FutureEither[A, BB] = {
    FutureEither(value.map(_.map(f))(ec))
  }
  def flatMap[BB](f: B => Future[Either[A, BB]])(implicit ec: ExecutionContext): FutureEither[A, BB] = {
    FutureEither(
      value.flatMap {
        case a @ Left(_) => Future.successful(a.asInstanceOf[Either[A, BB]])
        case Right(b) => f(b)
      }
    )
  }
}
```

```scala
val fe: FutureEither[DivideError, Int] = for {
  r1 <- FutureEither(divideAsyncEither(12, 2))
  r2 <- FutureEither(divideAsyncEither(r1, 2))
  r3 <- FutureEither(divideAsyncEither(r2, 2))
} yield r3
fe.value.onComplete {
  case Success(value) => value match {
    case Right(num) => println(num)
    case Left(Indivisible(n, d)) => println(s"$n は $d で割り切れません")
    case Left(ZeroDivision) => println("ゼロ割りはできません")
  }
  case Failure(e) => e.printStackTrace()
}
```

## CatsやScalazなどのライブラリでの例

* CatsやScalazなどの関数型ライブラリには、上記自作の `FutureEither` のようなクラス `EitherT` が用意されている
* さらに上記自作の `FutureEither` よりも一段抽象化されており、 `EitherT[Future, A, B]` のように `Future` の部分を別の型（Try, Idなど）を挿入することができる

### Cats

```scala
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import cats.data.EitherT
import cats.implicits._

import example.DivideError
import example.DivideError.Indivisible
import example.FutureEitherExample.divideAsyncEither

val e: EitherT[Future, DivideError, Int] = for {
  r1 <- EitherT(divideAsyncEither(12, 2))
  r2 <- EitherT(divideAsyncEither(r1, 2))
  r3 <- EitherT(divideAsyncEither(r2, 2))
} yield r3
val a: Future[Either[DivideError, Int]] = e.value
```

### Scalaz

```scala
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scalaz.EitherT
import scalaz.std.scalaFuture._

val e: EitherT[Future, DivideError, Int] = for {
  r1 <- EitherT.fromEither(divideAsyncEither(12, 2))
  r2 <- EitherT.fromEither(divideAsyncEither(r1, 2))
  r3 <- EitherT.fromEither(divideAsyncEither(r2, 2))
} yield r3
val a: Future[Either[DivideError, Int]] = e.run.map(_.toEither)
```

## EitherTを使う時のコツ

