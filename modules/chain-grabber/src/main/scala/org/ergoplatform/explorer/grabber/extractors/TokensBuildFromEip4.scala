package org.ergoplatform.explorer.grabber.extractors

import cats.Applicative
import org.ergoplatform.explorer.db.models.Token
import org.ergoplatform.explorer.grabber.models.SlotData
import org.ergoplatform.explorer.grabber.modules.BuildFrom
import org.ergoplatform.explorer.protocol.TokenPropsParser
import org.ergoplatform.explorer.protocol.models.ApiTransaction
import org.ergoplatform.explorer.{TokenId, TokenType}
import tofu.syntax.monadic._

final class TokensBuildFromEip4[F[_]: Applicative] extends BuildFrom[F, SlotData, List[Token]] {

  def apply(slot: SlotData): F[List[Token]] =
    slot.apiBlock.transactions.transactions.flatMap(tokensFrom).pure

  private def tokensFrom(tx: ApiTransaction): Option[Token] = {
    val allowedTokenId = TokenId.fromStringUnsafe(tx.inputs.head.boxId.value)
    for {
      out <- tx.outputs.toList.find(_.assets.map(_.tokenId).contains(allowedTokenId))
      props = TokenPropsParser.eip4.parse(out.additionalRegisters)
      asset <- out.assets.find(_.tokenId == allowedTokenId)
    } yield Token(
      asset.tokenId,
      out.boxId,
      asset.amount,
      props.map(_.name),
      props.map(_.description),
      props.map(_ => TokenType.Eip004),
      props.map(_.decimals)
    )
  }
}
