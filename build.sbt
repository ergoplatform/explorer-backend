lazy val commonSettings = Seq(
  scalacOptions ++= commonScalacOptions,
  scalaVersion := "2.12.10",
  organization := "org.ergoplatform",
  version := "0.0.1",
  resolvers += Resolver.sonatypeRepo("public"),
  resolvers += Resolver.sonatypeRepo("snapshots"),
  test in assembly := {},
  assemblyMergeStrategy in assembly := {
    case "logback.xml"                                => MergeStrategy.first
    case "module-info.class"                          => MergeStrategy.discard
    case other if other.contains("io.netty.versions") => MergeStrategy.first
    case other                                        => (assemblyMergeStrategy in assembly).value(other)
  },
  libraryDependencies ++= dependencies.CompilerPlugins
)

lazy val allConfigDependency = "compile->compile;test->test"

lazy val explorer = project
  .in(file("."))
  .withId("explorer-backend")
  .settings(commonSettings)
  .settings(moduleName := "explorer-backend", name := "ExplorerBackend")
  .aggregate(core, httpApi, grabber, utxWatcher, dex)

lazy val core = utils
  .mkModule("explorer-core", "ExplorerCore")
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= dependencies.core ++ dependencies.Testing
  )

lazy val httpApi = utils
  .mkModule("explorer-api", "ExplorerApi")
  .settings(commonSettings)
  .settings(
    mainClass in assembly := Some("org.ergoplatform.explorer.http.api.Application"),
    libraryDependencies ++= dependencies.api
  )
  .dependsOn(core)

lazy val grabber = utils
  .mkModule("chain-grabber", "ChainGrabber")
  .settings(commonSettings)
  .settings(
    mainClass in assembly := Some("org.ergoplatform.explorer.grabber.Application"),
    libraryDependencies ++= dependencies.grabber
  )
  .dependsOn(core % allConfigDependency)

lazy val utxWatcher = utils
  .mkModule("utx-watcher", "UtxWatcher")
  .settings(commonSettings)
  .settings(
    mainClass in assembly := Some("org.ergoplatform.explorer.watcher.Application"),
    libraryDependencies ++= dependencies.utxWatcher
  )
  .dependsOn(core)

lazy val utxBroadcaster = utils
  .mkModule("utx-broadcaster", "UtxBroadcaster")
  .settings(commonSettings)
  .settings(
    mainClass in assembly := Some("org.ergoplatform.explorer.broadcaster.Application"),
    libraryDependencies ++= dependencies.utxBroadcaster
  )
  .dependsOn(core)

lazy val dex = utils
  .mkModule("dex", "DEX")
  .settings(commonSettings)
  .settings(
    mainClass in assembly := Some("org.ergoplatform.explorer.dex.Application"),
    libraryDependencies ++= dependencies.Kafka
  )
  .dependsOn(core)

lazy val commonScalacOptions = List(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-language:higherKinds",
  "-language:postfixOps",
  "-language:implicitConversions",
  "-feature",
  "-unchecked",
  "-Xfuture",
  "-Yno-adapted-args",
  "-Ywarn-numeric-widen",
  "-Ypartial-unification"
)
