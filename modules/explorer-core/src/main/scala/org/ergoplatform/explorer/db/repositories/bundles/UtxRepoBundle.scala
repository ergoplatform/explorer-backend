package org.ergoplatform.explorer.db.repositories.bundles

import cats.Monad
import cats.effect.Concurrent
import cats.syntax.traverse._
import dev.profunktor.redis4cats.algebra.RedisCommands
import org.ergoplatform.explorer.cache.repositories.ErgoLikeTransactionRepo
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.repositories
import org.ergoplatform.explorer.db.repositories._
import org.ergoplatform.explorer.settings.UtxCacheSettings
import tofu.syntax.monadic._

final case class UtxRepoBundle[F[_], D[_], S[_[_], _]](
  txs: UTransactionRepo[D, S],
  inputs: UInputRepo[D, S],
  dataInputs: UDataInputRepo[D, S],
  outputs: UOutputRepo[D, S],
  confirmedOutputs: OutputRepo[D, S],
  assets: UAssetRepo[D],
  confirmedAssets: AssetRepo[D, S],
  ergoTxRepo: Option[ErgoLikeTransactionRepo[F, S]]
)

object UtxRepoBundle {

  def apply[F[_]: Concurrent, D[_]: Monad: LiftConnectionIO](
    utxCacheSettings: UtxCacheSettings,
    redis: Option[RedisCommands[F, String, String]]
  ): F[UtxRepoBundle[F, D, fs2.Stream]] =
    redis.map(ErgoLikeTransactionRepo[F](utxCacheSettings, _)).sequence.flatMap { etxRepo =>
      (
        UTransactionRepo[F, D],
        UInputRepo[F, D],
        UDataInputRepo[F, D],
        UOutputRepo[F, D],
        OutputRepo[F, D],
        UAssetRepo[F, D],
        AssetRepo[F, D]
      ).mapN(UtxRepoBundle(_, _, _, _, _, _, _, etxRepo))
    }
}
