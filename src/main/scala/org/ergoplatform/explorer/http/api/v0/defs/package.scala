package org.ergoplatform.explorer.http.api.v0

import cats.syntax.option._
import io.circe.generic.auto._
import org.ergoplatform.explorer.Err.ApiErr
import org.ergoplatform.explorer.http.api.models.Paging
import sttp.model.StatusCode
import sttp.tapir.json.circe._
import sttp.tapir._

package object defs {

  implicit private val codecErr: CodecForOptional[ApiErr, CodecFormat.Json, _] =
    implicitly[CodecForOptional[ApiErr, CodecFormat.Json, _]]

  val BaseEndpointPrefix: EndpointInput[Unit] = "api" / "v0"

  val baseEndpointDef: Endpoint[Unit, ApiErr, Unit, Nothing] =
    endpoint
      .in(BaseEndpointPrefix)
      .errorOut(
        oneOf(
          statusMapping(
            StatusCode.NotFound,
            jsonBody[ApiErr.NotFound].description("Not found")
          ),
          statusMapping(
            StatusCode.BadRequest,
            jsonBody[ApiErr.BadInput].description("Bad request")
          ),
          statusDefaultMapping(jsonBody[ApiErr].description("Unknown error"))
        )
      )

  val paging: EndpointInput[Paging] =
    (query[Option[Int]]("offset").validate(Validator.min(0).asOptionElement) and
     query[Option[Int]]("limit").validate(Validator.min(1).asOptionElement))
      .map {
        case (offsetOpt, limitOpt) =>
          Paging(offsetOpt.getOrElse(0), limitOpt.getOrElse(20))
      }
      { case Paging(offset, limit) => offset.some -> limit.some }
}
