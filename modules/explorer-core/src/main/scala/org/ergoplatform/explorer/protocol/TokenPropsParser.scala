package org.ergoplatform.explorer.protocol

import cats.instances.either._
import org.ergoplatform.explorer.protocol.models.TokenPropsEip4
import org.ergoplatform.explorer.{HexString, RegisterId}
import sigmastate.{SByte, SCollection}

import scala.util.Try

trait TokenPropsParser[PropsT] {
  def parse(registers: Map[RegisterId, HexString]): Option[PropsT]
}

object TokenPropsParser {

  private val Eip4StringCharset = "UTF-8"

  def eip4: TokenPropsParser[TokenPropsEip4] =
    new TokenPropsParser[TokenPropsEip4] {
      private val p = RegistersParser[Either[Throwable, *]]

      def parse(registers: Map[RegisterId, HexString]): Option[TokenPropsEip4] =
        for {
          r4             <- registers.get(RegisterId.R4)
          r5             <- registers.get(RegisterId.R5)
          r6             <- registers.get(RegisterId.R6)
          nameRaw        <- p.parse[SCollection[SByte.type]](r4).toOption
          descriptionRaw <- p.parse[SCollection[SByte.type]](r5).toOption
          decimalsRaw    <- p.parse[SCollection[SByte.type]](r6).toOption
          name           <- Try(new String(nameRaw.toArray, Eip4StringCharset)).toOption
          description    <- Try(new String(descriptionRaw.toArray, Eip4StringCharset)).toOption
          decimals       <- Try(new String(decimalsRaw.toArray, Eip4StringCharset).toInt).toOption
        } yield TokenPropsEip4(name, description, decimals)
    }
}
