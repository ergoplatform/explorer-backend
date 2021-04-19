import sbt.{CrossVersion, compilerPlugin, _}

object dependencies {

  import versions._

  val Http4s: List[ModuleID] =
    List(
      "org.http4s" %% "http4s-dsl",
      "org.http4s" %% "http4s-blaze-server",
      "org.http4s" %% "http4s-blaze-client",
      "org.http4s" %% "http4s-circe"
    ).map(_ % Http4sVersion)

  val Tapir: List[ModuleID] =
    List(
      "com.softwaremill.sttp.tapir" %% "tapir-core",
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe",
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server",
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs",
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml",
      "com.softwaremill.sttp.tapir" %% "tapir-redoc-http4s",
      "com.softwaremill.sttp.tapir" %% "tapir-enumeratum"
    ).map(_ % TapirVersion)

  val Circe: List[ModuleID] =
    List(
      "io.circe" %% "circe-core",
      "io.circe" %% "circe-generic",
      "io.circe" %% "circe-parser",
      "io.circe" %% "circe-refined"
    ).map(_       % CirceVersion) ++ List(
      "io.circe" %% "circe-magnolia-derivation" % CirceMagniliaDerivationVersion
    )

  val Cats: List[ModuleID] = List(
    "org.typelevel" %% "cats-core"   % CatsVersion,
    "org.typelevel" %% "cats-effect" % CatsEffectVersion,
    "org.typelevel" %% "cats-mtl"    % CatsMtlVersion,
    "org.typelevel" %% "mouse"       % MouseVersion
  )

  val Monix: List[ModuleID] = List(
    "io.monix" %% "monix" % MonixVersion
  )

  val Monocle: List[ModuleID] = List(
    "com.github.julien-truffaut" %% "monocle-core"  % MonocleVersion,
    "com.github.julien-truffaut" %% "monocle-macro" % MonocleVersion
  )

  val Fs2: List[ModuleID] = List(
    "co.fs2" %% "fs2-core" % Fs2Version
  )

  val Chimney: List[ModuleID] = List(
    "io.scalaland" %% "chimney" % ChimneyVersion
  )

  val Tofu: List[ModuleID] = List(
    "tf.tofu"     %% "tofu-core"        % TofuVersion,
    "tf.tofu"     %% "tofu-derivation"  % TofuVersion,
    "tf.tofu"     %% "tofu-logging"     % TofuVersion,
    "tf.tofu"     %% "tofu-fs2-interop" % TofuVersion,
    "org.manatki" %% "derevo-circe"     % DerevoVersion
  )

  val Ergo: List[ModuleID] = List(
    "org.ergoplatform" %% "ergo-wallet" % ErgoWalletVersion,
    "org.ergoplatform" %% "contracts"   % ErgoContractsVersion
  )

  val Logging: List[ModuleID] = List(
    "ch.qos.logback"     % "logback-classic" % Logback,
    "org.slf4j"          % "slf4j-api"       % Slf4j,
    "io.chrisdavenport" %% "log4cats-slf4j"  % Log4Cats
  )

  val Db: List[ModuleID] = List(
    "org.tpolecat" %% "doobie-core"     % DoobieVersion,
    "org.tpolecat" %% "doobie-postgres" % DoobieVersion,
    "org.tpolecat" %% "doobie-hikari"   % DoobieVersion,
    "org.tpolecat" %% "doobie-refined"  % DoobieVersion,
    "org.flywaydb"  % "flyway-core"     % FlywayVersion
  )

  val Testing: List[ModuleID] = List(
    "org.scalatest"              %% "scalatest"                       % ScalaTestVersion           % Test,
    "org.scalacheck"             %% "scalacheck"                      % ScalaCheckVersion          % Test,
    "com.dimafeng"               %% "testcontainers-scala-scalatest"  % TestContainersScalaVersion % Test,
    "com.dimafeng"               %% "testcontainers-scala-postgresql" % TestContainersScalaVersion % Test,
    "com.github.alexarchambault" %% "scalacheck-shapeless_1.14"       % ScalaCheckShapelessVersion % Test
  )

  val Typing: List[ModuleID] = List(
    "org.scalaz"  %% "deriving-macro" % DerivingVersion,
    "io.estatico" %% "newtype"        % NewtypeVersion,
    "eu.timepit"  %% "refined"        % RefinedVersion,
    "eu.timepit"  %% "refined-cats"   % RefinedVersion
  )

  val Enums: List[ModuleID] = List(
    "com.beachape" %% "enumeratum"       % EnumeratumVersion,
    "com.beachape" %% "enumeratum-circe" % EnumeratumCirceVersion
  )

  val Redis = List(
    "dev.profunktor" %% "redis4cats-effects" % CatsRedisVersion
  )

  val Config: List[ModuleID] = List(
    "com.github.pureconfig" %% "pureconfig"             % PureConfigVersion,
    "com.github.pureconfig" %% "pureconfig-cats-effect" % PureConfigVersion
  )

  val CompilerPlugins: List[ModuleID] =
    List(
      compilerPlugin(
        "org.typelevel" %% "kind-projector" % KindProjector cross CrossVersion.full
      ),
      compilerPlugin(
        "org.scalamacros" % "paradise" % MacroParadise cross CrossVersion.full
      ),
      compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
    )

  lazy val core: List[ModuleID] =
    Ergo ++
    Cats ++
    Tofu ++
    Fs2 ++
    Chimney ++
    Circe ++
    Http4s ++
    Tapir ++
    Db ++
    Config ++
    Logging ++
    Typing ++
    Monocle ++
    Redis ++
    Enums

  lazy val api: List[ModuleID] = Monix

  lazy val grabber: List[ModuleID] = Monix

  lazy val utxTracker: List[ModuleID] = Monix

  lazy val utxBroadcaster: List[ModuleID] = Monix
}
