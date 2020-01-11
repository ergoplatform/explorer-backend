package org.ergoplatform.explorer.http.api

import cats.data.NonEmptyMap
import cats.syntax.option._
import org.ergoplatform.explorer.http.api.models.{Paging, Sorting}
import sttp.tapir.{query, EndpointInput, Validator}

object commonDirectives {

  val paging: EndpointInput[Paging] =
    (query[Option[Int]]("offset").validate(Validator.min(0).asOptionElement) and
     query[Option[Int]]("limit").validate(Validator.min(1).asOptionElement))
      .map {
        case (offsetOpt, limitOpt) =>
          Paging(offsetOpt.getOrElse(0), limitOpt.getOrElse(20))
      } { case Paging(offset, limit) => offset.some -> limit.some }

  def sorting(
    allowedFields: NonEmptyMap[String, String],
    defaultField: Option[String] = None
  ): EndpointInput[Sorting] =
    (query[Option[String]]("sortBy").validate(
      Validator.`enum`(none :: allowedFields.keys.toNonEmptyList.toList.map(_.some))
    ) and query[Option[Sorting.SortOrder]]("sortDirection"))
      .map {
        case (fieldOpt, orderOpt) =>
          val field = fieldOpt getOrElse (defaultField getOrElse allowedFields.head._2)
          val ord   = orderOpt.getOrElse(Sorting.Desc)
          Sorting(fieldOpt.getOrElse(field), ord)
      } { case Sorting(sortBy, order) => sortBy.some -> order.some }
}
