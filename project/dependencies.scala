import sbt.{CrossVersion, compilerPlugin, _}

object dependencies {

  import versions._

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
        "com.softwaremill.sttp.tapir" %% "tapir-json-circe",
        "com.softwaremill.sttp.tapir" %% "tapir-http4s-server",
        "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs",
        "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml"
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
      "com.olegpy"    %% "meow-mtl-core"       % CatsMeowMtl,
      "org.typelevel" %% "cats-tagless-macros" % CatsTaglessVersion,
      "org.typelevel" %% "cats-tagless-core"   % CatsTaglessVersion,
      "org.typelevel" %% "mouse"               % MouseVersion
    )
  }

  object monix extends DependencyGroup {
    override def deps: List[ModuleID] = List(
      "io.monix" %% "monix" % MonixVersion,
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
      "ru.tinkoff" %% "tofu-core" % TofuVersion
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
      ),
      compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
    )

  lazy val core: List[ModuleID] = List(
    ergo.deps,
    cats.deps,
    tofu.deps,
    fs2.deps,
    circe.deps,
    http4s.deps,
    tapir.deps,
    db.deps,
    config.deps,
    logging.deps,
    newtypes.deps,
    simulacrum.deps,
    monocle.deps
  ).flatten

  lazy val api: List[ModuleID] = List(
    ergo.deps,
    cats.deps,
    monix.deps,
    fs2.deps,
    circe.deps,
    http4s.deps,
    tapir.deps,
    db.deps,
    logging.deps,
    newtypes.deps,
    simulacrum.deps,
    monocle.deps
  ).flatten

  lazy val grabber: List[ModuleID] = List(
    ergo.deps,
    cats.deps,
    monix.deps,
    fs2.deps,
    circe.deps,
    http4s.deps,
    tapir.deps,
    db.deps,
    logging.deps,
    newtypes.deps,
    simulacrum.deps,
    monocle.deps
  ).flatten

  lazy val utxWatcher: List[ModuleID] = List(
    ergo.deps,
    cats.deps,
    monix.deps,
    fs2.deps,
    circe.deps,
    http4s.deps,
    tapir.deps,
    db.deps,
    logging.deps,
    newtypes.deps,
    simulacrum.deps,
    monocle.deps
  ).flatten
}
