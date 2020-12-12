package org.ergoplatform.explorer.protocol

import cats.MonadError
import cats.syntax.flatMap._
import org.ergoplatform.explorer.HexString
import org.ergoplatform.explorer.protocol.models.RegisterValue
import sigmastate.Values.EvaluatedValue
import sigmastate.serialization.ValueSerializer

trait RegistersParser[F[_]] {

  def parse(raw: HexString): F[RegisterValue]
}

object RegistersParser {

  def apply[F[_]](implicit F: MonadError[F, Throwable]): RegistersParser[F] =
    (raw: HexString) =>
      F.catchNonFatal(ValueSerializer.deserialize(raw.bytes)).flatMap {
        case v: EvaluatedValue[_] => F.pure(RegisterValue(v.tpe.toTermString, v.value.toString))
        case v                    => F.raiseError(new Exception(s"Got non constant value [$v] in register"))
      }
}
