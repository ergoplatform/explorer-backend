package org.ergoplatform.explorer.http.api.v1.defs

import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.commonDirectives._
import org.ergoplatform.explorer.http.api.models.Epochs
import org.ergoplatform.explorer.http.api.v1.models.UnspentOutputInfo
import org.ergoplatform.explorer.settings.ServiceSettings
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir._

final class BoxesEndpointDefs[F[_]](settings: ServiceSettings) {

  private val PathPrefix = "boxes"

  def endpoints: List[Endpoint[_, _, _, _]] = streamUnspentOutputsByEpochsDef :: streamUnspentOutputsDef :: Nil

  def streamUnspentOutputsDef: Endpoint[Epochs, ApiErr, fs2.Stream[F, Byte], Fs2Streams[F]] =
    baseEndpointDef
      .in(PathPrefix / "unspent")
      .in(epochSlicing(settings.maxEpochsPerRequest))
      .out(streamBody(Fs2Streams[F], schemaFor[UnspentOutputInfo], CodecFormat.Json(), None))

  def streamUnspentOutputsByEpochsDef: Endpoint[Int, ApiErr, fs2.Stream[F, Byte], Fs2Streams[F]] =
    baseEndpointDef
      .in(PathPrefix / "unspent")
      .in(query[Int]("lastEpochs").validate(Validator.custom(validateEpochs(_, settings.maxEpochsPerRequest))))
      .out(streamBody(Fs2Streams[F], schemaFor[UnspentOutputInfo], CodecFormat.Json(), None))

  private def validateEpochs(numEpochs: Int, max: Int): List[ValidationError[_]] =
    if (numEpochs > max)
      ValidationError.Custom(
        numEpochs,
        s"To many epochs requested. Max allowed number is $max"
      ) :: Nil
    else Nil
}
