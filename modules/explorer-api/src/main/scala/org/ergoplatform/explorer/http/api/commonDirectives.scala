package org.ergoplatform.explorer.http.api

import cats.data.NonEmptyMap
import cats.instances.option._
import cats.syntax.flatMap._
import cats.syntax.option._
import org.ergoplatform.explorer.http.api.models.Sorting.SortOrder
import org.ergoplatform.explorer.http.api.models.{HeightRange, Paging, Sorting}
import sttp.tapir.{query, EndpointInput, ValidationError, Validator}

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

object commonDirectives {

  def paging: EndpointInput[Paging] = paging(Int.MaxValue)

  def paging(maxLimit: Int): EndpointInput[Paging] =
    (query[Option[Int]]("offset").validateOption(Validator.min(0)) and
      query[Option[Int]]("limit")
        .validateOption(Validator.min(1))
        .validateOption(Validator.max(maxLimit)))
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
      Validator.enumeration(none :: allowedFields.keys.toNonEmptyList.toList.map(_.some))
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

  def hideNfts: EndpointInput.Query[Boolean] =
    query[Boolean]("hideNfts")
      .default(false)
      .description("Exclude NFTs from result set")

  def blocksSlicing(maxBlocks: Int): EndpointInput[HeightRange] =
    (query[Int]("minHeight").validate(Validator.min(0)) and
      query[Int]("maxHeight").validate(Validator.min(1)))
      .validate(Validator.custom(validateBounds(_, maxBlocks)))
      .map(in => HeightRange(in._1, in._2))(epochs => epochs.minHeight -> epochs.maxHeight)

  def lastBlocks(maxBlocks: Int): EndpointInput.Query[Int] =
    query[Int]("lastEpochs").validate(Validator.max(maxBlocks))

  def limit(maxEntities: Int): EndpointInput.Query[Int] =
    query[Int]("limit").validate(Validator.max(maxEntities))

  def minGlobalIndex: EndpointInput.Query[Long] =
    query[Long]("minGix")
      .validate(Validator.min(0L))
      .description("Min global index (in blockchain) of an on-chain entity")

  def concise: EndpointInput.Query[Boolean] =
    query[Boolean]("concise")
      .default(false)
      .description("Hide excessive data")

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
