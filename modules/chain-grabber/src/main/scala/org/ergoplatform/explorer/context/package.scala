package org.ergoplatform.explorer

import tofu.{HasContext, HasLocal}

package object context {

  type HasGrabberContext[F[_], D[_]] = F HasContext GrabberContext[F, D]

  type HasSettings[F[_]] = F HasContext SettingsContext

  type HasRepo[F[_], Repo] = F HasLocal Repo
}
