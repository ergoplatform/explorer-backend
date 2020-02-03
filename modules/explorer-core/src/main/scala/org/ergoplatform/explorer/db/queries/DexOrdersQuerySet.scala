package org.ergoplatform.explorer.db.queries

import org.ergoplatform.explorer._
import org.ergoplatform.explorer.db.models.{Asset, Input, Output}
import org.ergoplatform.explorer.db.models.aggregates.ExtendedOutput

/** A set of queries for doobie implementation of [DexOrdersRepo].
  */
object DexOrdersQuerySet {

  import org.ergoplatform.explorer.db.models.schema.ctx._

  def getMainUnspentSellOrderByTokenId(
    tokenId: TokenId,
    ergoTreeTemplate: HexString,
    offset: Int,
    limit: Int
  ) =
    quote {
      query[Output]
        .join(query[Asset])
        .on {
          case (out, asset) => out.boxId == asset.boxId && asset.tokenId == lift(tokenId)
        }
        .leftJoin(query[Input])
        .on { case ((out, _), i) => i.boxId == out.boxId }
        .filter { case ((out, a), i) => out.mainChain }
        .filter {
          case ((out, _), _) =>
            infix"${out.ergoTree} like %${lift(ergoTreeTemplate.unwrapped)}"
              .as[Boolean]
        }
        .filter {
          case (_, i) => i.map(_.boxId).isEmpty || !i.getOrNull.mainChain
        }
        .drop(lift(offset))
        .take(lift(limit))
        .map {
          case ((out, _), i) =>
            ExtendedOutput(out, i.map(_.txId))
        }
    }

  def getMainUnspentBuyOrderByTokenId(
    tokenId: TokenId,
    ergoTreeTemplate: HexString,
    offset: Int,
    limit: Int
  ) = quote {
    query[Output]
      .leftJoin(query[Input])
      .on { case (out, i) => i.boxId == out.boxId }
      .filter { case (out, _) => out.mainChain }
      .filter {
        case (out, _) =>
          infix"${out.ergoTree} like %${lift(ergoTreeTemplate.unwrapped)}"
            .as[Boolean]
      }
      .filter {
        case (out, _) =>
          infix"${out.ergoTree} like %${lift(tokenId.value)}%"
            .as[Boolean]
      }
      .filter {
        case (_, i) => i.map(_.boxId).isEmpty || !i.getOrNull.mainChain
      }
      .drop(lift(offset))
      .take(lift(limit))
      .map {
        case (out, i) =>
          ExtendedOutput(out, i.map(_.txId))
      }
  }

}
