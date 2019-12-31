package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.ergoplatform.explorer.Id
import sttp.tapir.Schema
import sttp.tapir.generic.Derived

final case class BlockInfo(
  id: Id,
  height: Int,
  timestamp: Long,
  transactionsCount: Int,
  miner: MinerInfo,
  size: Int,
  difficulty: Long,
  minerReward: Long
)

object BlockInfo {

  implicit val codec: Codec[BlockInfo] = deriveCodec

  implicit val schema: Schema[BlockInfo] =
    implicitly[Derived[Schema[BlockInfo]]].value
      .modify(_.id)(_.description("Block ID"))
      .modify(_.height)(_.description("Block height"))
      .modify(_.timestamp)(_.description("Timestamp the block was created"))
      .modify(_.transactionsCount)(
        _.description("Number of transactions included in the block")
      )
      .modify(_.size)(_.description("Overall size of the block in bytes"))
      .modify(_.difficulty)(_.description("Block difficulty"))
      .modify(_.minerReward)(
        _.description("The amount of nanoErgs miner received as a reward for block")
      )

  def apply(blockInfo: org.ergoplatform.explorer.db.models.BlockInfo): BlockInfo = {
    val minerName = "" // todo:
    new BlockInfo(
      id                = blockInfo.headerId,
      height            = blockInfo.height,
      timestamp         = blockInfo.timestamp,
      transactionsCount = blockInfo.txsCount,
      miner             = MinerInfo(blockInfo.minerAddress, minerName),
      size              = blockInfo.blockSize,
      difficulty        = blockInfo.difficulty,
      minerReward       = blockInfo.minerReward
    )
  }
}
