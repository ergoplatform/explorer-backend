package org.ergoplatform.dex

import cats.effect.Concurrent
import fs2.concurrent.Queue
import fs2.{Pipe, Stream}
import org.ergoplatform.dex.domain.{Order, OrderType}
import org.ergoplatform.dex.streaming.Consumer
import org.ergoplatform.explorer.HexString
import org.ergoplatform.explorer.db.models.Output

abstract class OrdersWatcher[F[_], S[_[_] <: F[_], _]] {

  def run: S[F, Unit]
}

object OrdersWatcher {

  final private class Live[F[_]: Concurrent](consumer: Consumer[F, Stream], q: Queue[F, Order[_]])
    extends OrdersWatcher[F, Stream] {

    def run: Stream[F, Unit] =
      consumer.stream.through(process)

    private def process: Pipe[F, Output, Unit] =
      _.broadcastThrough(
        _.filter(out => isSellOrder(out.ergoTree)).evalMap(makeSellOrder),
        _.filter(out => isBuyOrder(out.ergoTree)).evalMap(makeBuyOrder)
      ).through(q.enqueue)

    private def makeSellOrder(output: Output): F[Order[OrderType.Sell]] = ???
    private def makeBuyOrder(output: Output): F[Order[OrderType.Buy]]   = ???

    private def isSellOrder(ergoTree: HexString): Boolean = ???
    private def isBuyOrder(ergoTree: HexString): Boolean  = ???
  }
}
