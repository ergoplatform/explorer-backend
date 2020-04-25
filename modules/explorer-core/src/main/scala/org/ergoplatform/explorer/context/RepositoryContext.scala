package org.ergoplatform.explorer.context

import cats.effect.Sync
import cats.syntax.apply._
import org.ergoplatform.explorer.LiftConnectionIO
import org.ergoplatform.explorer.db.repositories._
import tofu.optics.macros.ClassyOptics

@ClassyOptics("contains_")
final case class RepositoryContext[D[_], S[_[_], _]](
  adProofsRepo: AdProofRepo[D],
  assetRepo: AssetRepo[D, S],
  blockExtensionRepo: BlockExtensionRepo[D],
  blockInfoRepo: BlockInfoRepo[D],
  headerRepo: HeaderRepo[D],
  inputRepo: InputRepo[D],
  outputRepo: OutputRepo[D, S],
  transactionRepo: TransactionRepo[D, S],
  uAssetRepo: UAssetRepo[D],
  uInputRepo: UInputRepo[D, S],
  uOutputRepo: UOutputRepo[D, S],
  uTransactionRepo: UTransactionRepo[D, S]
)

object RepositoryContext {

  def apply[F[_]: Sync, D[_]: LiftConnectionIO]: F[RepositoryContext[D, fs2.Stream]] =
    (
      AdProofRepo[F, D],
      AssetRepo[F, D],
      BlockExtensionRepo[F, D],
      BlockInfoRepo[F, D],
      HeaderRepo[F, D],
      InputRepo[F, D],
      OutputRepo[F, D],
      TransactionRepo[F, D],
      UAssetRepo[F, D],
      UInputRepo[F, D],
      UOutputRepo[F, D],
      UTransactionRepo[F, D]
    ).mapN(RepositoryContext(_, _, _, _, _, _, _, _, _, _, _, _))
}
