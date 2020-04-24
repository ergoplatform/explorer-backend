package org.ergoplatform.dex

abstract class OrdersWatcher[F[_], S[_[_] <: F[_], _]] {

  def run: S[F, Unit]
}
