package org.ergoplatform.explorer.services

import cats.Monad
import cats.data.NonEmptyList
import cats.effect.Sync
import cats.effect.concurrent.Ref
import org.ergoplatform.explorer.UrlString
import tofu.syntax.monadic._

trait NodesPool[F[_]] {

  def getAll: F[NonEmptyList[UrlString]]

  def getBest: F[UrlString]

  def setBest(best: UrlString): F[Unit]

  def rotate: F[Unit]
}

object NodesPool {

  def apply[F[_]: Sync](nodes: NonEmptyList[UrlString]): F[NodesPool[F]] =
    Ref.of(nodes).map(new Live(_))

  final class Live[F[_]: Monad](poolRef: Ref[F, NonEmptyList[UrlString]]) extends NodesPool[F] {

    def getAll: F[NonEmptyList[UrlString]] = poolRef.get

    def getBest: F[UrlString] = poolRef.get map (_.head)

    def setBest(best: UrlString): F[Unit] =
      poolRef.update(p0 => NonEmptyList.of(best, p0.filterNot(_ == best): _*))

    def rotate: F[Unit] =
      poolRef.update(p0 => NonEmptyList.ofInitLast(p0.tail, p0.head))
  }
}
