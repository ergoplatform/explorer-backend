package org.ergoplatform.explorer

import cats.data.NonEmptyList
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Url

import scala.concurrent.duration.FiniteDuration

final case class Settings(
  chainPollInterval: FiniteDuration,
  masterNodesAddresses: NonEmptyList[String Refined Url]
)
