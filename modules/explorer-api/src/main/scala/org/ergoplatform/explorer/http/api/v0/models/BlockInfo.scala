package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.ergoplatform.explorer.Id
import org.ergoplatform.explorer.db.models.aggregates.ExtendedBlockInfo
import sttp.tapir.{Schema, Validator}

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
    Schema
      .derive[BlockInfo]
      .modify(_.id)(_.description("Block ID"))
      .modify(_.height)(_.description("Block height"))
      .modify(_.timestamp)(_.description("Timestamp the block was created (UNIX timestamp in millis)"))
      .modify(_.transactionsCount)(
        _.description("Number of transactions included in the block")
      )
      .modify(_.size)(_.description("Overall size of the block in bytes"))
      .modify(_.difficulty)(_.description("Block difficulty"))
      .modify(_.minerReward)(
        _.description("The amount of nanoErgs miner received as a reward for block")
      )

  implicit val validator: Validator[BlockInfo] = Validator.derive

  def apply(extBlockInfo: ExtendedBlockInfo): BlockInfo = {
    val minerName = extBlockInfo.minerNameOpt.getOrElse(
      extBlockInfo.blockInfo.minerAddress.unwrapped.takeRight(8)
    )
    new BlockInfo(
      id                = extBlockInfo.blockInfo.headerId,
      height            = extBlockInfo.blockInfo.height,
      timestamp         = extBlockInfo.blockInfo.timestamp,
      transactionsCount = extBlockInfo.blockInfo.txsCount,
      miner             = MinerInfo(extBlockInfo.blockInfo.minerAddress, minerName),
      size              = extBlockInfo.blockInfo.blockSize,
      difficulty        = extBlockInfo.blockInfo.difficulty,
      minerReward       = extBlockInfo.blockInfo.minerReward
    )
  }
}
