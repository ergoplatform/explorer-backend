import sbt.{CrossVersion, compilerPlugin, _}

object dependencies {

  val ErgoWalletVersion = "master-83d10111-SNAPSHOT"

  val Http4sVersion          = "0.21.0-M5"
  val TapirVersion           = "0.12.7"
  val CirceVersion           = "0.12.3"
  val CirceDerivationVersion = "0.12.0-M7"
  val CatsVersion            = "2.0.0"
  val CatsEffectVersion      = "2.0.0"
  val CatsMtlVersion         = "0.7.0"
  val CatsTaglessVersion     = "0.10"
  val MouseVersion           = "0.23"
  val Fs2Version             = "2.0.1"
  val DoobieVersion          = "0.8.4"
  val FlywayVersion          = "5.1.1"
  val ZioVersion             = "1.0.0-RC16"
  val ZioCatsVersion         = "2.0.0.0-RC6"
  val MonocleVersion         = "2.0.0"
  val TofuVersion            = "0.5.4"

  val SimulacrumVersion = "0.19.0"

  val Log4Cats = "0.3.0"
  val Logback  = "1.2.3"
  val Slf4j    = "1.7.25"

  val PureConfigVersion = "0.12.1"

  val NewtypeVersion  = "0.4.3"
  val RefinedVersion  = "0.9.10"
  val DerivingVersion = "1.0.0"

  val KindProjector = "0.11.0"
  val MacroParadise = "2.1.1"

  val ScalaTestVersion              = "3.0.8"
  val ScalaCheckVersion             = "1.14.1"
  val ScalaCheckShapelessVersion    = "1.2.3"
  val TestContainersPostgresVersion = "1.7.3"
  val TestContainersScalaVersion    = "0.18.0"

  trait DependencyGroup {
    def deps: List[ModuleID]
  }

  object http4s extends DependencyGroup {
    override def deps: List[ModuleID] =
      List(
        "org.http4s" %% "http4s-dsl",
        "org.http4s" %% "http4s-blaze-server",
        "org.http4s" %% "http4s-blaze-client",
        "org.http4s" %% "http4s-circe"
      ).map(_ % Http4sVersion)
  }

  object tapir extends DependencyGroup {
    override def deps: List[ModuleID] =
      List(
        "com.softwaremill.sttp.tapir" %% "tapir-core",
        "com.softwaremill.sttp.tapir" %% "tapir-json-circe"
      ).map(_ % TapirVersion)
  }

  object circe extends DependencyGroup {
    override def deps: List[ModuleID] =
      List(
        "io.circe" %% "circe-core",
        "io.circe" %% "circe-generic",
        "io.circe" %% "circe-parser",
        "io.circe" %% "circe-refined"
      ).map(_      % CirceVersion) ++ List(
        "io.circe" %% "circe-derivation" % CirceDerivationVersion
      )
  }

  object cats extends DependencyGroup {
    override def deps: List[ModuleID] = List(
      "org.typelevel" %% "cats-core"           % CatsVersion,
      "org.typelevel" %% "cats-effect"         % CatsEffectVersion,
      "org.typelevel" %% "cats-mtl-core"       % CatsMtlVersion,
      "org.typelevel" %% "cats-tagless-macros" % CatsTaglessVersion,
      "org.typelevel" %% "cats-tagless-core"   % CatsTaglessVersion,
      "org.typelevel" %% "mouse"               % MouseVersion
    )
  }

  object zio extends DependencyGroup {
    override def deps: List[ModuleID] = List(
      "dev.zio" %% "zio"              % ZioVersion,
      "dev.zio" %% "zio-interop-cats" % ZioCatsVersion
    )
  }

  object monocle extends DependencyGroup {
    override def deps: List[ModuleID] = List(
      "com.github.julien-truffaut" %% "monocle-core"  % MonocleVersion,
      "com.github.julien-truffaut" %% "monocle-macro" % MonocleVersion
    )
  }

  object fs2 extends DependencyGroup {
    override def deps: List[ModuleID] = List(
      "co.fs2" %% "fs2-core" % Fs2Version
    )
  }

  object tofu extends DependencyGroup {
    override def deps: List[ModuleID] = List(
      "ru.tinkoff" %% "tofu" % TofuVersion
    )
  }

  object ergo extends DependencyGroup {
    override def deps: List[ModuleID] = List(
      "org.ergoplatform" %% "ergo-wallet" % ErgoWalletVersion
    )
  }

  object logging extends DependencyGroup {
    override def deps: List[ModuleID] = List(
      "ch.qos.logback"    % "logback-classic" % Logback,
      "org.slf4j"         % "slf4j-api"       % Slf4j,
      "io.chrisdavenport" %% "log4cats-slf4j" % Log4Cats
    )
  }

  object db extends DependencyGroup {
    override def deps: List[ModuleID] = List(
      "org.tpolecat" %% "doobie-core"      % DoobieVersion,
      "org.tpolecat" %% "doobie-postgres"  % DoobieVersion,
      "org.tpolecat" %% "doobie-scalatest" % DoobieVersion,
      "org.tpolecat" %% "doobie-hikari"    % DoobieVersion,
      "org.tpolecat" %% "doobie-refined"   % DoobieVersion,
      "org.flywaydb" % "flyway-core"       % FlywayVersion
    )
  }

  object testing extends DependencyGroup {
    override def deps: List[ModuleID] = List(
      "org.tpolecat"               %% "doobie-scalatest"          % DoobieVersion                 % Test,
      "org.scalatest"              %% "scalatest"                 % ScalaTestVersion              % Test,
      "org.scalacheck"             %% "scalacheck"                % ScalaCheckVersion             % Test,
      "org.testcontainers"         % "postgresql"                 % TestContainersPostgresVersion % Test,
      "com.dimafeng"               %% "testcontainers-scala"      % TestContainersScalaVersion    % Test,
      "com.github.alexarchambault" %% "scalacheck-shapeless_1.14" % ScalaCheckShapelessVersion    % Test
    )
  }

  object newtypes extends DependencyGroup {
    override def deps: List[ModuleID] = List(
      "org.scalaz"  %% "deriving-macro" % DerivingVersion,
      "io.estatico" %% "newtype"        % NewtypeVersion,
      "eu.timepit"  %% "refined"        % RefinedVersion,
      "eu.timepit"  %% "refined-cats"   % RefinedVersion
    )
  }

  object config extends DependencyGroup {
    override def deps: List[ModuleID] = List(
      "com.github.pureconfig" %% "pureconfig"             % PureConfigVersion,
      "com.github.pureconfig" %% "pureconfig-cats-effect" % PureConfigVersion
    )
  }

  object simulacrum extends DependencyGroup {
    override def deps: List[ModuleID] = List(
      "com.github.mpilquist" %% "simulacrum" % SimulacrumVersion
    )
  }

  lazy val compilerPlugins: List[ModuleID] =
    List(
      compilerPlugin(
        "org.typelevel" %% "kind-projector" % KindProjector cross CrossVersion.full
      ),
      compilerPlugin(
        "org.scalamacros" % "paradise" % MacroParadise cross CrossVersion.full
      )
    )
}
