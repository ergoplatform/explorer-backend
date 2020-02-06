package org.ergoplatform.explorer

import scorex.util.encode.Base16

import scala.util.control.NoStackTrace

trait Err extends Exception with NoStackTrace {
  def msg: String
  override def getMessage: String = msg
}

object Err {

  final case class RefinementFailed(details: String) extends Err {
    val msg: String = s"Refinement failed: $details"
  }

  abstract class ProcessingErr(val msg: String) extends Err

  object ProcessingErr {

    final case class NoBlocksWritten(height: Int)
      extends ProcessingErr(s"No blocks written at height $height")

    final case class EcPointDecodingFailed(details: String)
      extends ProcessingErr(s"EcPoint decoding failed: $details")

    final case class TransactionDecodingFailed(json: String)
      extends ProcessingErr(s"Failed to decode transaction from json: $json")
  }

  abstract class RequestProcessingErr(val msg: String) extends Err

  object RequestProcessingErr {

    final case class InconsistentDbData(details: String)
      extends RequestProcessingErr(s"Inconsistent blockchain data in db: $details")

    final case class AddressDecodingFailed(
      address: Address,
      reasonOpt: Option[String] = None
    ) extends RequestProcessingErr(
        s"Failed to decode address: `$address`" + reasonOpt
          .map(s => s", reason: $s")
          .getOrElse("")
      )

    final case class Base16DecodingFailed(
      hexString: HexString,
      reasonOpt: Option[String] = None
    ) extends RequestProcessingErr(
        s"Failed to decode Base16: `$hexString`" + reasonOpt
          .map(s => s", reason: $s")
          .getOrElse("")
      )

    final case class ErgoTreeDeserializationFailed(
      bytes: Array[Byte],
      reasonOpt: Option[String] = None
    ) extends RequestProcessingErr(
        s"Failed to deserialize ergo tree from: `${Base16.encode(bytes)}`" + reasonOpt
          .map(s => s", reason: $s")
          .getOrElse("")
      )
  }

  abstract class DexErr(override val msg: String) extends RequestProcessingErr(msg)

  object DexErr {

    final case class DexSellOrderAttributesFailed(details: String) extends DexErr(details)

    final case class DexBuyOrderAttributesFailed(details: String) extends DexErr(details)

  }
}
