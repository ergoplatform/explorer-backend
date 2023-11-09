lazy val commonSettings = Seq(
  scalacOptions ++= commonScalacOptions,
  scalaVersion := "2.12.15",
  organization := "org.ergoplatform",
  version := "9.17.9",
  resolvers += Resolver.sonatypeRepo("public"),
  resolvers += Resolver.sonatypeRepo("snapshots"),
  libraryDependencies ++= dependencies.Testing ++ dependencies.CompilerPlugins,
  ThisBuild / evictionErrorLevel := Level.Info,
  assembly / test := {},
  assembly / assemblyMergeStrategy := {
    case "logback.xml"                                             => MergeStrategy.first
    case "module-info.class"                                       => MergeStrategy.discard
    case other if other.contains("scala/annotation/nowarn.class")  => MergeStrategy.first
    case other if other.contains("scala/annotation/nowarn$.class") => MergeStrategy.first
    case other if other.contains("io.netty.versions")              => MergeStrategy.first
    case other                                                     => (assembly / assemblyMergeStrategy).value(other)
  }
)

lazy val allConfigDependency = "compile->compile;test->test"

lazy val explorer = project
  .in(file("."))
  .withId("explorer-backend")
  .settings(commonSettings)
  .settings(moduleName := "explorer-backend", name := "ExplorerBackend")
  .aggregate(core, httpApi, grabber, utxTracker, utxBroadcaster, migrator)

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
    assembly / mainClass := Some("org.ergoplatform.explorer.http.api.Application"),
    libraryDependencies ++= dependencies.api
  )
  .dependsOn(core)

lazy val grabber = utils
  .mkModule("chain-grabber", "ChainGrabber")
  .settings(commonSettings)
  .settings(
    assembly / mainClass := Some("org.ergoplatform.explorer.indexer.Application"),
    libraryDependencies ++= dependencies.grabber
  )
  .dependsOn(core % allConfigDependency)

lazy val utxTracker = utils
  .mkModule("utx-tracker", "UtxTracker")
  .settings(commonSettings)
  .settings(
    assembly / mainClass := Some("org.ergoplatform.explorer.tracker.Application"),
    libraryDependencies ++= dependencies.utxTracker
  )
  .dependsOn(core)

lazy val utxBroadcaster = utils
  .mkModule("utx-broadcaster", "UtxBroadcaster")
  .settings(commonSettings)
  .settings(
    assembly / mainClass := Some("org.ergoplatform.explorer.broadcaster.Application"),
    libraryDependencies ++= dependencies.utxBroadcaster
  )
  .dependsOn(core)

lazy val migrator = utils
  .mkModule("migrator", "Migrator")
  .settings(commonSettings)
  .settings(
    assembly / mainClass := Some("org.ergoplatform.explorer.migration.Application")
  )
  .dependsOn(grabber, httpApi)

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
  "-Ypartial-unification"
)
