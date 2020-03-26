package org.ergoplatform.explorer.settings

import cats.syntax.either._
import cats.syntax.list._
import cats.data.NonEmptyList
import eu.timepit.refined.api.{Refined, Validate}
import eu.timepit.refined._
import pureconfig.ConfigReader
import pureconfig.error.CannotConvert

object pureConfigInstances {

  implicit def configReaderForRefined[A: ConfigReader, P](
    implicit v: Validate[A, P]
  ): ConfigReader[A Refined P] =
    ConfigReader[A].emap { a =>
      refineV[P](a).leftMap(r => CannotConvert(a.toString, s"Refined", r))
    }

  implicit def nelReader[A: ConfigReader]: ConfigReader[NonEmptyList[A]] =
    implicitly[ConfigReader[List[A]]].emap { list =>
      list.toNel.toRight(CannotConvert(list.toString, s"NonEmptyList", "List is empty"))
    }
}
