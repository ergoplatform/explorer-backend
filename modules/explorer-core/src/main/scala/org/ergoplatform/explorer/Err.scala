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

    final case class InconsistentNodeView(details: String)
      extends ProcessingErr(details)

    final case class EcPointDecodingFailed(details: String)
      extends ProcessingErr(s"EcPoint decoding failed: $details")

    final case class TransactionDecodingFailed(json: String)
      extends ProcessingErr(s"Failed to decode transaction from json: $json")
  }

  abstract class RequestProcessingErr(val msg: String) extends Err

  object RequestProcessingErr {

    final case class BadRequest(details: String)
      extends RequestProcessingErr(s"Bad request: $details")

    final case class FeatureNotSupported(details: String)
      extends RequestProcessingErr(s"Feature not supported: $details")

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

    abstract class NetworkErr(msg: String) extends RequestProcessingErr(msg)

    object NetworkErr {

      final case class InvalidTransaction(id: String)
        extends NetworkErr(s"Transaction with id '$id' declined by the network")

      final case class TransactionSubmissionFailed(id: String)
        extends NetworkErr(s"Failed to submit transaction with id '$id' to the network")

      final case class RequestFailed(urls: List[UrlString])
        extends NetworkErr(s"Failed to execute request, URLs tried ${urls.mkString(", ")}")
    }

    abstract class DexErr(msg: String) extends RequestProcessingErr(msg)

    object DexErr {

      abstract class ContractParsingErr(msg: String) extends DexErr(msg)

      object ContractParsingErr {

        final case class Base16DecodingFailed(
          hexString: HexString,
          reasonOpt: Option[String] = None
        ) extends ContractParsingErr(
            s"Failed to decode Base16: `$hexString`" + reasonOpt
              .map(s => s", reason: $s")
              .getOrElse("")
          )

        abstract class ErgoTreeSerializationErr(msg: String) extends ContractParsingErr(msg)

        object ErgoTreeSerializationErr {

          final case class ErgoTreeDeserializationFailed(
            bytes: Array[Byte],
            reasonOpt: Option[String] = None
          ) extends ErgoTreeSerializationErr(
              s"Failed to deserialize ergo tree from: `${Base16.encode(bytes)}`" + reasonOpt
                .map(s => s", reason: $s")
                .getOrElse("")
            )

          final case class ErgoTreeSerializationFailed(
            ergoTree: ErgoTree,
            reasonOpt: Option[String] = None
          ) extends ErgoTreeSerializationErr(
              s"Failed to serialize ergo tree: `$ergoTree`" + reasonOpt
                .map(s => s", reason: $s")
                .getOrElse("")
            )
        }
      }

      final case class DexContractInstantiationFailed(details: String) extends DexErr(details)

      final case class DexSellOrderAttributesFailed(details: String) extends DexErr(details)

      final case class DexBuyOrderAttributesFailed(details: String) extends DexErr(details)
    }
  }
}
