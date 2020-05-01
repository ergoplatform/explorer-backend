package org.ergoplatform.dex.domain

sealed abstract class OrderType

object OrderType {
  final case class Buy() extends OrderType
  final case class Sell() extends OrderType
}
