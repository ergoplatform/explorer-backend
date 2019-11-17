package org.ergoplatform.explorer

import cats.data.NonEmptyList
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Url

final case class Settings(masterNodesAddresses: NonEmptyList[String Refined Url])
