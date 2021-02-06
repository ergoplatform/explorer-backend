package org.ergoplatform.explorer

import cats.{Applicative, FlatMap, Monad}
import shapeless.{::, Generic, HList, HNil}
import tofu.syntax.monadic._

trait BuildFrom[F[_], A, B] {

  def apply(a: A): F[B]
}

object BuildFrom {

  def instance[F[_], A, B](f: A => F[B]): BuildFrom[F, A, B] =
    (a: A) => f(a)

  def pure[F[_]: Applicative, A, B](f: A => B): BuildFrom[F, A, B] =
    (a: A) => f(a).pure

  implicit def extractHNil[F[_]: Applicative, A]: BuildFrom[F, A, HNil] =
    instance(_ => HNil.pure.widen[HNil])

  implicit def extractHList[F[_]: FlatMap, A, H, T <: HList](implicit
    hExtract: BuildFrom[F, A, H],
    tExtract: BuildFrom[F, A, T]
  ): BuildFrom[F, A, H :: T] =
    instance { a =>
      hExtract(a) >>= (h => tExtract(a).map(h :: _))
    }

  implicit def instance[F[_], A, B, R](implicit
    F: Monad[F],
    gen: Generic.Aux[B, R],
    extract: BuildFrom[F, A, R]
  ): BuildFrom[F, A, B] =
    instance(a => extract(a).map(gen.from))

  object syntax {

    implicit final class BuildFromOps[A](private val a: A) extends AnyVal {
      def intoF[F[_], B](implicit ev: BuildFrom[F, A, B]): F[B] = ev(a)
    }
  }
}
