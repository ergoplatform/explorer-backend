package org.ergoplatform.explorer.settings

import cats.effect.Sync
import pureconfig.module.catseffect._
import pureconfig.{ConfigReader, ConfigSource}

import scala.reflect.ClassTag

trait SettingCompanion[T] {

  def load[F[_]: Sync](pathOpt: Option[String])(implicit
    r: ConfigReader[T],
    ct: ClassTag[T]
  ): F[T] =
    pathOpt
      .map(ConfigSource.file(_).withFallback(ConfigSource.default))
      .getOrElse(ConfigSource.default)
      .loadF[F, T]
}
