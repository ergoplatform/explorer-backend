package org.ergoplatform.explorer

import cats.arrow.FunctionK
import org.ergoplatform.explorer.clients.ergo.ErgoNetworkClient
import org.ergoplatform.explorer.db.Trans
import tofu.optics.{Contains, Extract}
import tofu.{Context, HasContext, HasLocal, WithContext}

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

  @inline def reposCtx[F[_]: HasRepos]: F[RepositoryContext[F, fs2.Stream]] =
    tofu.syntax.context.context[F]

  @inline def transCtx[F[_], D[_]](
    implicit
    F: HasGrabberContext[F, D],
    lens: GrabberContext[F, D] Extract Trans[D, F]
  ): F[D Trans F] = Context[F].extract(lens).context

  @inline def settingsCtx[F[_], D[_]](
    implicit
    F: HasGrabberContext[F, D],
    lens: GrabberContext[F, D] Extract SettingsContext
  ): F[SettingsContext] = Context[F].extract(lens).context

  @inline def networkClientCtx[F[_], D[_]](
    implicit
    F: HasGrabberContext[F, D],
    lens: GrabberContext[F, D] Extract ErgoNetworkClient[F, fs2.Stream]
  ): F[ErgoNetworkClient[F, fs2.Stream]] = Context[F].extract(lens).context
}
