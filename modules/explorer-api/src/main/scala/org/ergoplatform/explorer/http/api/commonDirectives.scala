package org.ergoplatform.explorer.http.api

import java.util.concurrent.TimeUnit

import cats.data.NonEmptyMap
import cats.syntax.option._
import cats.syntax.flatMap._
import cats.instances.option._
import org.ergoplatform.explorer.http.api.models.{Paging, Sorting}
import sttp.tapir.{query, EndpointInput, Validator}

import scala.concurrent.duration.FiniteDuration

object commonDirectives {

  val paging: EndpointInput[Paging] =
    (query[Option[Int]]("offset").validate(Validator.min(0).asOptionElement) and
    query[Option[Int]]("limit").validate(Validator.min(1).asOptionElement))
      .map {
        case (offsetOpt, limitOpt) =>
          Paging(offsetOpt.getOrElse(0), limitOpt.getOrElse(20))
      } { case Paging(offset, limit) => offset.some -> limit.some }

  val confirmations: EndpointInput[Int] =
    query[Option[Int]]("minConfirmations").map(_.getOrElse(0))(_.some)

  val timespan: EndpointInput[FiniteDuration] =
    query[Option[String]]("timespan").map {
      _.flatMap(parseTimespan).getOrElse(FiniteDuration(365, TimeUnit.DAYS))
    } { _.toString.some }

  def sorting(
    allowedFields: NonEmptyMap[String, String],
    defaultFieldOpt: Option[String] = None
  ): EndpointInput[Sorting] =
    (query[Option[String]]("sortBy").validate(
      Validator.`enum`(none :: allowedFields.keys.toNonEmptyList.toList.map(_.some))
    ) and query[Option[Sorting.SortOrder]]("sortDirection"))
      .map {
        case (fieldOpt, orderOpt) =>
          val specFieldOpt = fieldOpt >>= (allowedFields(_))
          val field        = specFieldOpt getOrElse (defaultFieldOpt getOrElse allowedFields.head._2)
          val ord          = orderOpt getOrElse Sorting.Desc
          Sorting(field, ord)
      } {
        case Sorting(sortBy, order) =>
          allowedFields.toNel.find(_._2 == sortBy).map(_._1) -> order.some
      }

  private val TimespanRegex = "^([0-9]+)(days|day|years|year)$".r

  private def parseTimespan(s: String): Option[FiniteDuration] =
    s.trim.toLowerCase match {
      case TimespanRegex(x, "days" | "day")   => FiniteDuration(x.toLong, TimeUnit.DAYS).some
      case TimespanRegex(x, "years" | "year") => FiniteDuration(x.toLong * 365, TimeUnit.DAYS).some
      case _                                  => none
    }
}
