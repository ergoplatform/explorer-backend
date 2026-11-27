package org.ergoplatform.explorer.protocol

import cats.MonadError
import org.ergoplatform.explorer.HexString
import org.ergoplatform.explorer.protocol.models.RegisterValue
import sigma.VersionContext
import sigma.ast.{EvaluatedValue, SType}
import sigma.serialization.ValueSerializer
import tofu.syntax.monadic._
import tofu.syntax.raise._

import scala.reflect.ClassTag

trait RegistersParser[F[_]] {

  def parseAny(raw: HexString): F[RegisterValue]

  def parse[T <: SType](raw: HexString)(implicit ev: ClassTag[T#WrappedType]): F[T#WrappedType]
}

object RegistersParser {

  private val v6Version: Byte = VersionContext.V6SoftForkVersion

  def apply[F[_]](implicit F: MonadError[F, Throwable]): RegistersParser[F] =
    new RegistersParser[F] {

      def parseAny(raw: HexString): F[RegisterValue] =
        F.catchNonFatal(
          VersionContext.withVersions(v6Version, v6Version) {
            ValueSerializer.deserialize(raw.bytes)
          }
        ).flatMap {
          case v: EvaluatedValue[_] =>
            F.catchNonFatal(sigmaWrappers.renderEvaluatedValue(v)).flatMap {
              _.map { case (tp, vl) => RegisterValue(tp, vl) }
                .orRaise[F](new Exception(s"Failed to render constant value [$v] in register"))
            }
          case v => F.raiseError(new Exception(s"Got non constant value [$v] in register"))
        }

      def parse[T <: SType](raw: HexString)(implicit ev: ClassTag[T#WrappedType]): F[T#WrappedType] =
        F.catchNonFatal(
          VersionContext.withVersions(v6Version, v6Version) {
            ValueSerializer.deserialize(raw.bytes)
          }
        ).flatMap {
          case v: EvaluatedValue[_] =>
            v.value match {
              case wrappedValue: T#WrappedType => F.pure(wrappedValue)
              case wrappedValue =>
                F.raiseError(new Exception(s"Got wrapped value [$wrappedValue] of unexpected type in register"))
            }
          case v => F.raiseError(new Exception(s"Got non constant value [$v] in register"))
        }
    }
}
