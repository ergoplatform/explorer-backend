package org.ergoplatform.explorer

import tofu.optics.Contains
import tofu.{HasContext, HasLocal, WithContext}

package object context {

  type HasGrabberContext[F[_], D[_]] = F HasContext GrabberContext[F, D]

  type HasSettings[F[_]] = F HasContext SettingsContext

  type HasRepo[F[_], Repo] = F HasLocal Repo

  type HasRepos[D[_]] = D HasContext RepositoryContext[D, fs2.Stream]

  implicit def extractContext[F[_], D[_], A](
    implicit
    F: HasGrabberContext[F, D],
    lens: GrabberContext[F, D] Contains A
  ): F WithContext A =
    F.extract(lens)
}
