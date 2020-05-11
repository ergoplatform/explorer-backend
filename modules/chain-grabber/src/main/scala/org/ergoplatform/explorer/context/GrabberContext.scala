package org.ergoplatform.explorer.context

import org.ergoplatform.explorer.clients.ergo.ErgoNetworkClient
import org.ergoplatform.explorer.db.Trans
import tofu.optics.macros.{promote, ClassyOptics}

@ClassyOptics
final case class GrabberContext[F[_], D[_]](
  @promote settings: SettingsContext,
  networkClient: ErgoNetworkClient[F, fs2.Stream],
  @promote trans: D Trans F
)
