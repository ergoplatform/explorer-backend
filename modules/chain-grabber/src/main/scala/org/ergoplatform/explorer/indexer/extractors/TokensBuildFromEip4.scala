package org.ergoplatform.explorer.indexer.extractors

import cats.Applicative
import org.ergoplatform.explorer.db.models.Token
import org.ergoplatform.explorer.indexer.models.SlotData
import org.ergoplatform.explorer.protocol.TokenPropsParser
import org.ergoplatform.explorer.protocol.models.ApiTransaction
import org.ergoplatform.explorer.{BuildFrom, TokenId, TokenType}
import tofu.syntax.monadic._

final class TokensBuildFromEip4[F[_]: Applicative] extends BuildFrom[F, SlotData, List[Token]] {

  def apply(slot: SlotData): F[List[Token]] =
    slot.apiBlock.transactions.transactions.flatMap(tokensFrom).pure

  private def tokensFrom(tx: ApiTransaction): Option[Token] = {
    val allowedTokenId = TokenId.fromStringUnsafe(tx.inputs.head.boxId.value)
    for {
      out <- tx.outputs.toList.find(_.assets.map(_.tokenId).contains(allowedTokenId))
      props  = TokenPropsParser.eip4Partial.parse(out.additionalRegisters)
      assets = out.assets.filter(_.tokenId == allowedTokenId)
      headAsset <- assets.headOption
      assetAmount = assets.map(_.amount).sum
    } yield Token(
      headAsset.tokenId,
      out.boxId,
      assetAmount,
      props.map(_.name),
      props.map(_.description),
      props.map(_ => TokenType.Eip004),
      props.map(_.decimals)
    )
  }
}
