package org.ergoplatform.explorer.http.api.v1.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.ergoplatform.explorer.BlockId
import org.ergoplatform.explorer.db.models.aggregates.ExtendedBlockInfo
import org.ergoplatform.explorer.protocol.blocks
import sttp.tapir.{Schema, Validator}

final case class BlockInfo(
  id: BlockId,
  height: Int,
  epoch: Int,
  version: Byte,
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
      .derived[BlockInfo]
      .modify(_.id)(_.description("Block ID"))
      .modify(_.height)(_.description("Block height"))
      .modify(_.epoch)(_.description("Block epoch (Epochs are enumerated from 0)"))
      .modify(_.version)(_.description("Block version"))
      .modify(_.timestamp)(_.description("Timestamp the block was created (UNIX timestamp in millis)"))
      .modify(_.transactionsCount)(
        _.description("Number of transactions included in the block")
      )
      .modify(_.size)(_.description("Overall size of the block in bytes"))
      .modify(_.difficulty)(_.description("Block difficulty"))
      .modify(_.minerReward)(
        _.description("The amount of nanoErgs miner received as a reward for block")
      )

  implicit val validator: Validator[BlockInfo] = schema.validator

  def apply(block: ExtendedBlockInfo): BlockInfo = {
    val minerName = block.minerNameOpt.getOrElse(
      block.blockInfo.minerAddress.unwrapped.takeRight(8)
    )
    new BlockInfo(
      id                = block.blockInfo.headerId,
      height            = block.blockInfo.height,
      epoch             = blocks.epochOf(block.blockInfo.height),
      version           = block.blockVersion,
      timestamp         = block.blockInfo.timestamp,
      transactionsCount = block.blockInfo.txsCount,
      miner             = MinerInfo(block.blockInfo.minerAddress, minerName),
      size              = block.blockInfo.blockSize,
      difficulty        = block.blockInfo.difficulty,
      minerReward       = block.blockInfo.minerReward
    )
  }
}
