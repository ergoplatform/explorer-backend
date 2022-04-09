package org.ergoplatform.explorer.cache

import cats.effect._
import cats.effect.concurrent.Ref
import cats.effect.implicits._
import cats.implicits._
import dev.profunktor.redis4cats.algebra._
import io.chrisdavenport.log4cats.Logger

object redisTransaction {

  case class Transaction[F[_]: Concurrent: Logger, K, V](
    cmd: RedisCommands[F, K, V]
  ) {

    private val logger = Logger[F]

    /***
      * Exclusively run Redis commands as part of a transaction.
      *
      * Every command needs to be forked (`.start`) to be sent to the server asynchronously.
      * After a transaction is complete, either successfully or with a failure, the spawned
      * fibers will be treated accordingly.
      *
      * It should not be used to run other computations, only Redis commands. Fail to do so
      * may end in unexpected results such as a dead lock.
      */
    def run(commands: F[Unit]*): F[Unit] =
      Ref.of[F, List[Fiber[F, Unit]]](List.empty).flatMap { fibers =>
        Ref.of[F, Boolean](false).flatMap { txFailed =>
          val tx =
            Resource.makeCase(cmd.multi) {
              case (_, ExitCase.Completed) =>
                cmd.exec *> logger.info("Transaction completed")
              case (_, ExitCase.Error(e)) =>
                cmd.discard.guarantee(txFailed.set(true)) *>
                logger.error(s"Transaction failed: ${e.getMessage}")
              case (_, ExitCase.Canceled) =>
                cmd.discard.guarantee(txFailed.set(true)) *>
                logger.error("Transaction canceled")
            }

          val joinOrCancelFibers =
            fibers.get.flatMap { fbs =>
              txFailed.get.ifM(
                fbs.traverse(_.cancel).void,
                fbs.traverse(_.join).void
              )
            }

          logger.info("Transaction started") >>
          tx.use(_ => commands.toList.traverse(_.start).flatMap(fibers.set))
            .guarantee(joinOrCancelFibers)
        }
      }
  }
}
