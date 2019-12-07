# はじめてのEitherT

この記事は [Scala Advent Calendar 2019](https://qiita.com/advent-calendar/2019/scala) の7日目の記事です。

* 対象読者: `Either` や `Future` が分かってきたScalaエンジニア
* わたしについて: Scalaを書いたり、PlayFrameworkでwebアプリケーションを書いたりするお仕事をしている
  ([Twitter: @mi12cp](https://twitter.com/mi12cp))
* 2019/12/07 時点の最新の安定板 Scala 2.13.1, Cats 2.0.0, Scalaz 7.2.29 を使っている
* この記事で扱うこと
  * `Either`, `Future` のおさらい、`Future[Either[A, B]]`を合成する
  * 自作の `FutureEither[A, B]` クラスを作成する
  * Cats, Scalazの `EitherT[Future, A, B]` を使う
  * `EitherT` を使ってアプリケーションを構築するときのコツ
* この記事で扱わないこと
  * モナドが云々
  * モナドトランスフォーマーが云々、モナドが合成出来る云々
  * 型クラスやContext Boundが云々
* この記事のまとめ: `Future[Either[A, B]]` は CatsやScalazなどを使って `EitherT[Future, A, B]` に変換すると合成が簡単にできる

## これまでのあらすじ

* `Either[A, B]` を使うとエラーハンドリングが簡単に書けて便利！
* `Future[T]` を使うと非同期処理が簡単に書けて便利！
* ということは、 `Future[Either[A, B]]` は非同期処理もエラーハンドリングも出来て最高なのでは？

### Eitherのおさらい

* `Either` は成功時の値か失敗時の値のどちらかを表すことができる
* 例外を投げる代わりに、失敗を意味する値(`case class` や `object`)を扱うことでスマートに記述できる

```scala
/**
 * 整数同士を割るメソッド
 * 割り切れる場合 Right で商を返す
 * 割り切れない場合 Indivisible 、ゼロ割りの場合 ZeroDivision を Left で返す
 */
def divideEither(num: Int, denom: Int): Either[DivideError, Int] = {
  if (denom == 0) Left(ZeroDivision)
  else if (num % denom != 0) Left(Indivisible(num, denom))
  else Right(num / denom)
}

sealed trait DivideError
object DivideError {
  case class Indivisible(num: Int, denom: Int) extends DivideError
  object ZeroDivision extends DivideError
}
```

```scala
divideEither(4, 2) // Right(2)
divideEither(4, 3) // Left(Indivisible(4,3))
divideEither(4, 0) // Left(ZeroDivision)
```

* `Either` に対して、さらに失敗する可能性のある処理を `flatMap` で合成することができる
  * `Right` であれば処理を進める
  * `Left` であればこれ以上何もしない
* ここでは `flatMap` の中に `Int => Either[DivideError, Int]` である関数を入れる

```scala
val res1: Either[DivideError, Int] = divideEither(12, 2)
// res1 == Right(6)
val res2: Either[DivideError, Int] = res1.flatMap { r => divideEither(r, 2) }
// res2 == Right(3)
val res3: Either[DivideError, Int] = res2.flatMap { r => divideEither(r, 2) }
// res3 == Left(Indivisible(3, 2))
val res4: Either[DivideError, Int] = res3.flatMap { r => divideEither(r, 2) }
// res4 == Left(Indivisible(3, 2)), res3の時点で Left なのでこれ以上結果は変わらない
```

* 上記の `flatMap` はfor式でより簡潔に書くことができる

```scala
val result: Either[DivideError, Int] = for {
  res1 <- divideEither(12, 2)
  res2 <- divideEither(res1, 2)
  res3 <- divideEither(res2, 2)
  res4 <- divideEither(res3, 2)
} yield res4
// result == Left(Indivisible(3, 2))
```

ここでは簡単に整数を割るだけのメソッドを作ったが、`Either` は色々なサービスのエラーハンドリングに使えそうだ

### Futureのおさらい

* `Future` は非同期処理を簡単に行なうことのできるツール
* `Future` にくるまれた値は、ある時点において利用可能になる可能性のある値を保持する
* `Future` を行うような処理はデータベースへの書き込み・ネットワーク越しの通信・ディスクへの書き込みなど例外が発生することが多い。
  * 例外がなければ結果は `Success` に包まれる
  * 例外が発生したら例外が `Failure` に包まれる

```scala
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure}

def divideAsync(num: Int, denom: Int): Future[Int] = Future {
  if (denom == 0) throw new IllegalArgumentException("denom must not be 0")
  if (num % denom != 0) throw new Exception(s"$num is not divisible by $denom")
  num / denom
}
```

```scala
val a: Future[Int] = divideAsync(4, 2)
a.onComplete {
  case Success(value) => println("devided: " + value) // ここでは value は Int
  case Failure(e) => println(e)
}
// "devided: 2" が出力される

val b = divideAsync(1, 0)
b.onComplete {
  case Success(value) => println(value)
  case Failure(e) => println(e) // e は java.lang.Throwable
}
// "java.lang.IllegalArgumentException: denom must not be 0" が出力される
```

* `Future` も合成できる
  * `Success` であれば処理を進める
  * `Failure` であればこれ以上何もしない
* ここでは `flatMap` の中に `Int => Future[Int]` である関数を入れる

```scala
val res1: Future[Int] = divideAsync(12, 2)
val res2: Future[Int] = res1.flatMap { r => divideAsync(r, 2) }
val res3: Future[Int] = res2.flatMap { r => divideAsync(r, 2) }
val res4: Future[Int] = res3.flatMap { r => divideAsync(r, 2) }
res4.onComplete {
  case Success(value) => println(value)
  case Failure(e) => println(e)
}
// "java.lang.Exception: 3 is not divisible by 2" が出力される
```

* 上記の `flatMap` はfor式でより簡潔に書くことができる

```scala
val result: Future[Int] = for {
  res1 <- divideAsync(12, 2)
  res2 <- divideAsync(res1, 2)
  res3 <- divideAsync(res2, 2)
  res4 <- divideAsync(res3, 2)
} yield res4
result.onComplete {
  case Success(value) => println(value)
  case Failure(e) => println(e)
}
// "java.lang.Exception: 3 is not divisible by 2" が出力される
```

ここでは簡単に整数を割るだけのメソッドを作ったが、`Future` は色々なサービスの非同期処理の合成に使えそうだ

しかし実際のサービスでは `Failure` にデータベースへの書き込み・ネットワーク越しの通信・ディスクへの書き込みなど例外が入ることがあるので、
上記 `Either` の時の `Indivisible` や `ZeroDivision` のような既知のエラーとは区別したい


### Future[Either[A, B]] を使う

* `Either` も `Future` も便利そうなので、同時に使ってしまおう

```scala
def divideAsyncEither(num: Int, denom: Int): Future[Either[DivideError, Int]] = {
  Future(divideEither(num, denom))
}
```

```scala
val result = divideAsyncEither(4, 2)
result.onComplete {
  case Success(v) => v match {
    case Right(num) => println(num)
    case Left(Indivisible(n, d)) => println(s"$n is not divisible by $d")
    case Left(ZeroDivision) => println("denom must not be 0")
  }
  case Failure(e) => println(e)
}
```

* しかし合成が面倒。
  `Future[Either[DivideError, Int]]` の `Future` の部分に対して `flatMap` を行うので、
  `Either[DivideError, Int]` の部分に対しては自分でパターンマッチを行わなければならない

```scala
val result: Future[Either[DivideError, Int]] = for {
  res1 <- divideAsyncEither(12, 2)
  res2 <- res1 match {
    case l @ Left(_) => Future.successful(l)
    case Right(r) => divideAsyncEither(r, 2)
  }
  res3 <- res2 match {
    case l @ Left(_) => Future.successful(l)
    case Right(r) => divideAsyncEither(r, 2)
  }
} yield res3
```

## Future[Either[A, B]] の合成を簡単にするためのクラスを作る

`FutureEither` という `case class` を作成する

* 方針1. `Future[Either[A, B]]` である値をラップする
  * `FutureEither(divideAsyncEither(12, 2))` のようにしてインスタンスを作成できる
* 方針2. `FutureEither` は `Future` が `Success`, `Either` が `Right` の時 `flatMap` が出来る

```scala
import scala.concurrent.{Future, ExecutionContext}

case class FutureEither[A, B](value: Future[Either[A, B]]) {

  def map[BB](f: B => BB)(implicit ec: ExecutionContext): FutureEither[A, BB] = {
    FutureEither(value.map(_.map(f))(ec))
  }

  def flatMap[BB](f: B => Future[Either[A, BB]])(implicit ec: ExecutionContext): FutureEither[A, BB] = {
    FutureEither(
      value.flatMap {
        case l @ Left(_) => Future.successful(l.asInstanceOf[Either[A, BB]])
        case Right(r) => f(r).value
      }
    )
  }

}
```

```scala
// Future[Either[DivideError, Int]] である divideAsyncEither を FutureEitherで包んで、
// FutureEither の flatMapを叩いている
val fe: FutureEither[DivideError, Int] = for {
  res1 <- FutureEither(divideAsyncEither(12, 2))
  res2 <- FutureEither(divideAsyncEither(res1, 2))
  res3 <- FutureEither(divideAsyncEither(res2, 2))
} yield res3

// FutureEither から Future[Either[DivideError, Int]] に戻す
val result: Future[Either[Divide, Int]] = fe.value

// あとは同じ
result.onComplete {
  case Success(v) => v match {
    case Right(num) => println(num)
    case Left(Indivisible(n, d)) => println(s"$n is not divisible by $d")
    case Left(ZeroDivision) => println("denom must not be 0")
  }
  case Failure(e) => e.printStackTrace()
}
```

合成が便利になった！

しかしこういったユーティリティは既存のライブラリにありそうですね

## CatsやScalazなどのライブラリでの例

* CatsやScalazなどの関数型ライブラリには、上記の `FutureEither[A, B]` を抽象化した `EitherT[F[_], A, B]` が用意されている
* `F[_]` の部分に `Future` を入れて `EitherT[Future, A, B]` のようにすると 上記の `FutureEither[A, B]` のように扱える


### Cats(2.0.0) での例

* 上記の `FutureEither` と同じような感じ
* `import cats.implicits._` をする必要がある

```scala
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import cats.data.EitherT
import cats.implicits._

val e: EitherT[Future, DivideError, Int] = for {
  r1 <- EitherT(divideAsyncEither(12, 2))
  r2 <- EitherT(divideAsyncEither(r1, 2))
  r3 <- EitherT(divideAsyncEither(r2, 2))
} yield r3
val a: Future[Either[DivideError, Int]] = e.value
```

### Scalaz(7.2.29)

* `import scalaz.std.scalaFuture._` をする必要がある
* Scalazでは `Either` の代わりに `\/` を使うので、 `EitherT.fromEither` で `Future[Either[A, B]]` を入れる
* `EitherT` から中の `Future[\/[A, B]]` を取り出すのに `run` を使い、さらに `_.map(_.toEither)` で `Future[Either[A, B]]` に戻せる

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

以下では明記していない限りCatsを使っていきます

## EitherTを使う時のコツ

* 覚える `EitherT` のメソッドは少なくしていきたいので色々工夫する

### EitherT[Future, FooErr, Foo]とEitherT[Future, BarErr, Bar]を合成する

* `leftMap` を使ってleftの型を揃えましょう

```scala
trait Foo
sealed trait FooErr
object FooErr extends FooErr
trait FooRepository {
  def findBy(fooId: Long): Future[Either[FooErr, Foo]]
}

trait Bar
sealed trait BarErr
object BarErr extends BarErr
trait BarRepository {
  def findBy(barId: Long): Future[Either[BarErr, Bar]]
}

case class FooBar(foo: Foo, bar: Bar)
sealed trait FooBarErr
object FooBarErr extends FooBarErr
class FooBarServise(fooRepository: FooRepository, barRepository: BarRepository) {
  def findFooBar(fooId: Long, barId: Long): Future[Either[FooBarErr, FooBar]] = {
    val e = for {
      foo <- EitherT(fooRepository.findBy(fooId)).leftMap {
        case FooErr => FooBarErr
      }
      bar <- EitherT(barRepository.findBy(barId)).leftMap {
        case BarErr => FooBarErr
      }
    } yield FooBar(foo, bar)
    e.value
  }
}
```


### Future[Option[T]] をEitherTで合成したい

#### Future[Option[T]] の代わりに Future[Either[A, B]] を返すように変更してしまう

```scala
trait UserRepository {
  def findBy(userId: Long): Future[Option[User]]
}
```

のような型のメソッドが生えていることが多いけれど、後続の処理でEitherTで回したくなることが多いので

```scala
trait UserRepository {
  import UserRepository._
  def findBy(userId: Long): Future[Either[FindByError, User]]
}
object UserRepository {
  trait FindByError
  object FindByError {
    case object UserNotFound extends FindByError
  }
}
```

のようにあらかじめ `Option` ではなく、単一の `Left` を返す `Either` にしておくと楽なことが多い

また、 `Either` にしておくと

```scala
object FindByError {
  case object UserNotFound extends FindByError
  case object UserBlocked extends FindByError
}
```

のように `FindByError` のエラーの種類を増やした時に変更が少ないという利点もある

#### どうしても `Future[Option[T]]` から変更できないのですが…

Catsには `EitherT.fromOption` が生えていたりするけれど、それを使わなくてもScala標準のOptionの `toRight` を使えば良いような気がする

```scala
def findBy: Future[Option[User]]
def findByEither: Future[Either[String, User]] = a.map(_.toRight("when Option is None"))
```

### EitherTで合成した後はFuture[Either[A, B]]に戻して返す

```scala
def divideAsyncEither(num: Int, denom: Int): Future[Either[DivideError, Int]]
```

みたいなのを複数回叩くように合成したメソッドを作る時は、

```scala
def divideAsyncEitherThreeTimes(num: Int, denom: Int): EitherT[Future, DivideError, Int] = {
  for {
    r1 <- EitherT(divideAsyncEither(num, denom))
    r2 <- EitherT(divideAsyncEither(r1, denom))
    r3 <- EitherT(divideAsyncEither(r2, denom))
  } yield r3
}
```

のように `EitherT` のまま返すではなく、

```scala
def divideAsyncEitherThreeTimes(num: Int, denom: Int): Future[Either[DivideError, Int]] = {
  val e = for {
    r1 <- EitherT(divideAsyncEither(12, 2))
    r2 <- EitherT(divideAsyncEither(r1, 2))
    r3 <- EitherT(divideAsyncEither(r2, 2))
  } yield r3
  e.value
}
```

のように `Future[Either[A, B]]` に戻してから返すように統一することで、
あるメソッドが `EitherT[Future[A, B]]` なのか `Future[Eituer[A, B]]` なのか混乱することがなくなる

また、 `EitherT` で返すと利用側はCatsやScalaz（または自作のライブラリ）に依存しなければならないが、
`Future[Either[A, B]]` に戻すことで利用側が自由に扱えるようになる

`EitherT` に包んだり取り出したりするコストは微々たるものなので何度でも包んだり取り出したりしちゃいましょう

## おわりに

みんなで楽しいEitherTライフを！
