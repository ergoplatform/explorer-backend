lazy val commonSettings = Seq(
  scalacOptions ++= commonScalacOptions,
  scalaVersion := "2.12.12",
  organization := "org.ergoplatform",
  version := "5.0.0-M3",
  resolvers += Resolver.sonatypeRepo("public"),
  resolvers += Resolver.sonatypeRepo("snapshots"),
  libraryDependencies ++= dependencies.Testing ++ dependencies.CompilerPlugins,
  test in assembly := {},
  assemblyMergeStrategy in assembly := {
    case "logback.xml"                                => MergeStrategy.first
    case "module-info.class"                          => MergeStrategy.discard
    case other if other.contains("io.netty.versions") => MergeStrategy.first
    case other                                        => (assemblyMergeStrategy in assembly).value(other)
  }
)

lazy val allConfigDependency = "compile->compile;test->test"

lazy val explorer = project
  .in(file("."))
  .withId("explorer-backend")
  .settings(commonSettings)
  .settings(moduleName := "explorer-backend", name := "ExplorerBackend")
  .aggregate(core, httpApi, grabber, utxWatcher, utxBroadcaster)

lazy val core = utils
  .mkModule("explorer-core", "ExplorerCore")
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= dependencies.core
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

lazy val v5migrator = utils
  .mkModule("v5-migrator", "V5Migrator")
  .settings(commonSettings)
  .settings(
    mainClass in assembly := Some("org.ergoplatform.explorer.migration.v5.Application")
  )
  .dependsOn(grabber)

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
