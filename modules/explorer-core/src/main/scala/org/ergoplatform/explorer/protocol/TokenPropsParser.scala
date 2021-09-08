package org.ergoplatform.explorer.protocol

import cats.instances.either._
import org.ergoplatform.explorer.protocol.models.TokenPropsEip4
import org.ergoplatform.explorer.{HexString, RegisterId}
import sigmastate.{SByte, SCollection}
import tofu.syntax.monadic._

import scala.util.Try

trait TokenPropsParser[PropsT] {
  def parse(registers: Map[RegisterId, HexString]): Option[PropsT]
}

object TokenPropsParser {

  private val Eip4StringCharset = "UTF-8"

  def eip4Partial: TokenPropsParser[TokenPropsEip4] =
    new TokenPropsParser[TokenPropsEip4] {
      private val p = RegistersParser[Either[Throwable, *]]

      def parse(registers: Map[RegisterId, HexString]): Option[TokenPropsEip4] = {
        def parse(raw: HexString) = p.parse[SCollection[SByte.type]](raw).toOption
        val r4                    = registers.get(RegisterId.R4)
        val r5                    = registers.get(RegisterId.R5)
        val r6                    = registers.get(RegisterId.R6)
        val nameRaw               = r4 >>= parse
        val nameOpt               = nameRaw >>= (raw => Try(new String(raw.toArray, Eip4StringCharset)).toOption)
        nameOpt.map { name =>
          val descriptionRaw          = r5 >>= parse
          val decimalsRaw             = r6 >>= parse
          val descriptionOpt          = descriptionRaw >>= (raw => Try(new String(raw.toArray, Eip4StringCharset)).toOption)
          val decimalsOpt             = decimalsRaw >>= (raw => Try(new String(raw.toArray, Eip4StringCharset).toInt).toOption)
          val (description, decimals) = (descriptionOpt.getOrElse(""), decimalsOpt.getOrElse(0))
          TokenPropsEip4(name, description, decimals)
        }
      }
    }
}
