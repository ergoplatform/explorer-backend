package org.ergoplatform.explorer

import cats.data.NonEmptyList
import cats.instances.try_._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.refineV
import eu.timepit.refined.string.ValidByte
import org.ergoplatform.explorer.settings.{ProtocolSettings, Settings}
import org.ergoplatform.settings.MonetarySettings

import scala.concurrent.duration._
import scala.util.Try

trait MainNetConfiguration {

  val GenesisAddress: String =
  "2Z4YBkDsDvQj8BX7xiySFewjitqp2ge9c99jfes2whbtKitZTxdBYqbrVZUvZvKv6aqn9by4kp3LE1c26LCyosFnVnm6b6U1JYv" +
  "WpYmL2ZnixJbXLjWAWuBThV1D6dLpqZJYQHYDznJCk49g5TUiS4q8khpag2aNmHwREV7JSsypHdHLgJT7MGaw51aJfNubyzSK" +
  "xZ4AJXFS27EfXwyCLzW1K6GVqwkJtCoPvrcLqmqwacAWJPkmh78nke9H4oT88XmSbRt2n9aWZjosiZCafZ4osUDxmZcc5QVEe" +
  "TWn8drSraY3eFKe8Mu9MSCcVU"
  val MainNetPrefix: Refined[String, ValidByte] = refineV[ValidByte]("16").right.get

  val monetarySettings = MonetarySettings()
  val protocolSettings = ProtocolSettings(MainNetPrefix, GenesisAddress, monetarySettings)
  val mainnetNodes     = NonEmptyList.one(UrlString.fromString[Try]("http://139.59.29.87:9053").get)
  val settings         = Settings(1.second, mainnetNodes, protocolSettings)
}
