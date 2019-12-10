
name := "explorer-backend"

organization := "org.ergoplatform"

version := "0.0.1"

scalaVersion := "2.12.10"

resolvers += Resolver.sonatypeRepo("public")
resolvers += Resolver.sonatypeRepo("snapshots")

lazy val projectDeps = List(
  dependencies.ergo.deps,
  dependencies.cats.deps,
  dependencies.zio.deps,
  dependencies.fs2.deps,
  dependencies.circe.deps,
  dependencies.http4s.deps,
  dependencies.db.deps,
  dependencies.logging.deps,
  dependencies.newtypes.deps,
  dependencies.simulacrum.deps,
  dependencies.monocle.deps
).flatten

libraryDependencies ++= projectDeps ++ dependencies.testing.deps ++ dependencies.compilerPlugins

scalacOptions ++= Seq(
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

test in assembly := {}
assemblyMergeStrategy in assembly := {
  case "logback.xml" => MergeStrategy.first
  case "module-info.class" => MergeStrategy.discard
  case other => (assemblyMergeStrategy in assembly).value(other)
}

