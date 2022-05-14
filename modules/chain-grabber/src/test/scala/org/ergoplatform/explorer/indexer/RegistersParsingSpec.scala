package org.ergoplatform.explorer.indexer

import cats.data.NonEmptyList
import cats.effect.IO
import eu.timepit.refined.refineMV
import org.ergoplatform.explorer.{Address, BlockId, HexString, TxId}
import org.ergoplatform.explorer.indexer.models.SlotData
import org.ergoplatform.explorer.protocol.models.{
  ApiBlockTransactions,
  ApiFullBlock,
  ApiHeader,
  ApiOutput,
  ApiTransaction
}
import org.ergoplatform.explorer.settings.ProtocolSettings
import org.ergoplatform.settings.MonetarySettings
import org.scalatest.{Matchers, PropSpec}
import tofu.{Context, WithContext}

class RegistersParsingSpec extends PropSpec with Matchers {
  property("All R4-R9 NonMandatoryRegisters are parsed") {
    val Right(apiOutput) = io.circe.parser.decode[ApiOutput](sample)
    val List(output) =
      org.ergoplatform.explorer.indexer.extractors.outputsBuildFrom[IO].apply(slot(apiOutput)).unsafeRunSync()
    output.additionalRegisters.noSpacesSortKeys shouldBe
    "{\"R4\":{\"renderedValue\":\"()\",\"serializedValue\":\"62\",\"sigmaType\":\"SUnit\"}," +
    "\"R5\":{\"renderedValue\":\"()\",\"serializedValue\":\"62\",\"sigmaType\":\"SUnit\"}," +
    "\"R6\":{\"renderedValue\":\"()\",\"serializedValue\":\"62\",\"sigmaType\":\"SUnit\"}," +
    "\"R7\":{\"renderedValue\":\"()\",\"serializedValue\":\"62\",\"sigmaType\":\"SUnit\"}," +
    "\"R8\":{\"renderedValue\":\"()\",\"serializedValue\":\"62\",\"sigmaType\":\"SUnit\"}," +
    "\"R9\":{\"renderedValue\":null,\"serializedValue\":\"3c0e400e03505250022e30633430306530313034313135393666373537323230366336663631366532303461363136653735363137323739\",\"sigmaType\":null}}"
  }

  def prevBlockId     = BlockId.fromStringUnsafe("66dbe6cf7c4ab9fb76aeea5bf58851d1f97d4ee27a24ef2af4b9b155a88fc5ae")
  def blockId         = BlockId.fromStringUnsafe("5c388f43c7d1f341746f15549b313f072b5275ae2c0fa5b864fd279c850490f3")
  def sampleHexString = HexString.fromStringUnsafe("5c388f43c7d1f341746f15549b313f072b5275ae2c0fa5b864fd279c850490f3")

  def slot = (o: ApiOutput) =>
    SlotData(
      ApiFullBlock(
        header = ApiHeader(
          id               = blockId,
          parentId         = prevBlockId,
          version          = 0,
          height           = 10000,
          nBits            = 100,
          difficulty       = null,
          timestamp        = 10000000L,
          stateRoot        = sampleHexString,
          adProofsRoot     = sampleHexString,
          transactionsRoot = sampleHexString,
          extensionHash    = sampleHexString,
          minerPk          = sampleHexString,
          w                = sampleHexString,
          n                = sampleHexString,
          d                = null,
          votes            = null
        ),
        transactions = ApiBlockTransactions(
          blockId,
          List(
            ApiTransaction(
              TxId("5c388f43c7d1f341746f15549b313f072b5275ae2c0fa5b864fd279c850490f3"),
              null,
              List(),
              NonEmptyList.one(o),
              1000
            )
          )
        ),
        extension = null,
        adProofs  = null,
        size      = 1000
      ),
      None
    )

  implicit def ctx: WithContext[IO, ProtocolSettings] = Context.const(
    ProtocolSettings(
      refineMV("0"),
      Address.fromStringUnsafe(
        "2Z4YBkDsDvQj8BX7xiySFewjitqp2ge9c99jfes2whbtKitZTxdBYqbrVZUvZvKv6aqn9by4kp3LE1c26LCyosFnVnm6b6U1JYvWpYmL2ZnixJbXLjWAWuBThV1D6dLpqZJYQHYDznJCk49g5TUiS4q8khpag2aNmHwREV7JSsypHdHLgJT7MGaw51aJfNubyzSKxZ4AJXFS27EfXwyCLzW1K6GVqwkJtCoPvrcLqmqwacAWJPkmh78nke9H4oT88XmSbRt2n9aWZjosiZCafZ4osUDxmZcc5QVEeTWn8drSraY3eFKe8Mu9MSCcVU"
      ),
      MonetarySettings()
    )
  )

  lazy val sample =
    """
      |{
      | "boxId":"80a8b3a328d45ef468c27b1f5b0e0d924bfd52c16c1058aaaddd5d66ecf98610",
      | "value":1000000000,
      | "index":0,
      | "creationHeight":195701,
      | "ergoTree":"0008cd02b26b5d0e874589c7c5754a6a6af408081fffffe311184f44a25b80c7f2bc62b5",
      | "assets":[],
      | "additionalRegisters":{
      |   "R4":"62",
      |   "R5":"62",
      |   "R6":"62",
      |   "R7":"62",
      |   "R8":"62",
      |   "R9":"3c0e400e03505250022e30633430306530313034313135393666373537323230366336663631366532303461363136653735363137323739"
      | }
      |}
      |""".stripMargin
}
