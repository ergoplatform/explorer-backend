package org.ergoplatform.explorer.http.api

import java.util.concurrent.TimeUnit

import cats.data.NonEmptyMap
import cats.instances.option._
import cats.syntax.flatMap._
import cats.syntax.option._
import org.ergoplatform.explorer.http.api.models.Sorting.SortOrder
import org.ergoplatform.explorer.http.api.models.{Epochs, Paging, Sorting}
import sttp.tapir.{EndpointInput, ValidationError, Validator, query}

import scala.concurrent.duration.FiniteDuration

object commonDirectives {

  def paging: EndpointInput[Paging] = paging(Int.MaxValue)

  def paging(maxLimit: Int): EndpointInput[Paging] =
    (query[Option[Int]]("offset").validate(Validator.min(0).asOptionElement) and
      query[Option[Int]]("limit")
        .validate(Validator.min(1).asOptionElement)
        .validate(Validator.max(maxLimit).asOptionElement))
      .map { input =>
        Paging(input._1.getOrElse(0), input._2.getOrElse(20))
      } { case Paging(offset, limit) => offset.some -> limit.some }

  val confirmations: EndpointInput[Int] =
    query[Option[Int]]("minConfirmations").map(_.getOrElse(0))(_.some)

  val timespan: EndpointInput[FiniteDuration] =
    query[Option[String]]("timespan").map {
      _.flatMap(parseTimespan).getOrElse(FiniteDuration(365, TimeUnit.DAYS))
    }(_.toString.some)

  def sorting(
    allowedFields: NonEmptyMap[String, String],
    defaultFieldOpt: Option[String] = None
  ): EndpointInput[Sorting] =
    (query[Option[String]]("sortBy").validate(
      Validator.`enum`(none :: allowedFields.keys.toNonEmptyList.toList.map(_.some))
    ) and query[Option[Sorting.SortOrder]]("sortDirection"))
      .map { input =>
        val (fieldOpt, orderOpt) = input
        val specFieldOpt         = fieldOpt >>= (allowedFields(_))
        val field                = specFieldOpt getOrElse (defaultFieldOpt getOrElse allowedFields.head._2)
        val ord                  = orderOpt getOrElse Sorting.Desc
        Sorting(field, ord)
      } { case Sorting(sortBy, order) =>
        allowedFields.toNel.find(_._2 == sortBy).map(_._1) -> order.some
      }

  def ordering: EndpointInput[SortOrder] =
    query[Option[Sorting.SortOrder]]("sortDirection")
      .map(_ getOrElse Sorting.Desc) { ordering =>
        ordering.some
      }

  def epochSlicing(maxEpochs: Int): EndpointInput[Epochs] =
    (query[Int]("minHeight").validate(Validator.min(0)) and
      query[Int]("maxHeight").validate(Validator.min(1)))
      .validate(Validator.custom(validateBounds(_, maxEpochs)))
      .map(in => Epochs(in._1, in._2))(epochs => epochs.minHeight -> epochs.maxHeight)

  def lastEpochs(maxEpochs: Int): EndpointInput.Query[Int] =
    query[Int]("lastEpochs").validate(Validator.max(maxEpochs))

  private def validateBounds(bounds: (Int, Int), max: Int): List[ValidationError[_]] =
    bounds match {
      case (minH, maxH) if maxH - minH > max =>
        ValidationError.Custom(
          bounds,
          s"To many epochs requested. Max allowed number is '$max'"
        ) :: Nil
      case _ => Nil
    }

  private val TimespanRegex = "^([0-9]+)(days|day|years|year)$".r

  private def parseTimespan(s: String): Option[FiniteDuration] =
    s.trim.toLowerCase match {
      case TimespanRegex(x, "days" | "day")   => FiniteDuration(x.toLong, TimeUnit.DAYS).some
      case TimespanRegex(x, "years" | "year") => FiniteDuration(x.toLong * 365, TimeUnit.DAYS).some
      case _                                  => none
    }
}
