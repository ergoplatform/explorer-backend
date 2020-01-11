lazy val commonSettings = Seq(
  scalacOptions ++= commonScalacOptions,
  scalaVersion := "2.12.10",
  organization := "org.ergoplatform",
  version := "0.0.1",
  resolvers += Resolver.sonatypeRepo("public"),
  resolvers += Resolver.sonatypeRepo("snapshots")
)

lazy val syncConfig = project
  .in(file("."))
  .withId("explorer-backend")
  .settings(commonSettings)
  .settings(moduleName := "explorer-backend", name := "ExplorerBackend")
  .aggregate(core, httpApi, grabber, utxWatcher)

lazy val core = utils
  .mkModule("explorer-core", "ExplorerCore")
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= dependencies.core ++ dependencies.testing.deps ++ dependencies.compilerPlugins
  )

lazy val httpApi = utils
  .mkModule("explorer-api", "ExplorerApi")
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= dependencies.api ++ dependencies.testing.deps ++ dependencies.compilerPlugins
  )
  .dependsOn(core)

lazy val grabber = utils
  .mkModule("chain-grabber", "ChainGrabber")
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= dependencies.grabber ++ dependencies.testing.deps ++ dependencies.compilerPlugins
  )
  .dependsOn(core)

lazy val utxWatcher = utils
  .mkModule("utx-watcher", "UtxWatcher")
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= dependencies.utxWatcher ++ dependencies.testing.deps ++ dependencies.compilerPlugins
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
