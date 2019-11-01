package org.ergoplatform

import doobie.util.{Get, Put}
import doobie.refined.implicits._
import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.string._
import io.estatico.newtype.{Coercible, NewType}
import io.estatico.newtype.ops._

package object explorer {

  sealed class DoobieInstances[A: Get: Put] {
    implicit def get[T: Coercible[A, *]]: Get[T] = Get[A].map(_.coerce)
    implicit def put[T]: Put[T] = Put[A].contramap(Coercible.instance(_))
  }

  object StringTagDoobieInstances extends DoobieInstances[NonEmptyString]

  type Id = Id.Type

  object Id extends NewType.Default[NonEmptyString] {
    // doobie instances
    implicit def get: Get[Type] = StringTagDoobieInstances.get
    implicit def put: Put[Type] = StringTagDoobieInstances.put
  }

  type TxId = TxId.Type

  object TxId extends NewType.Default[NonEmptyString] {
    // doobie instances
    implicit def get: Get[Type] = StringTagDoobieInstances.get
    implicit def put: Put[Type] = StringTagDoobieInstances.put
  }

  type BoxId = BoxId.Type

  object BoxId extends NewType.Default[NonEmptyString] {
    // doobie instances
    implicit def get: Get[Type] = StringTagDoobieInstances.get
    implicit def put: Put[Type] = StringTagDoobieInstances.put
  }

  type AssetId = AssetId.Type

  object AssetId extends NewType.Default[NonEmptyString] {
    // doobie instances
    implicit def get: Get[Type] = StringTagDoobieInstances.get
    implicit def put: Put[Type] = StringTagDoobieInstances.put
  }

  type Address = Base58String

  type HexString = String Refined MatchesRegex[W.`"[0-9a-fA-F]+"`.T]

  type Base58String = String Refined MatchesRegex[W.`"[1-9A-HJ-NP-Za-km-z]+"`.T]

  type NonEmptyString = String Refined NonEmpty
}
