name := "explorer-backend"

organization := "org.ergoplatform"

version := "0.0.1"

scalaVersion := "2.12.10"

resolvers += Resolver.sonatypeRepo("public")
resolvers += Resolver.sonatypeRepo("snapshots")

lazy val ergoWalletVersion = "master-83d10111-SNAPSHOT"
lazy val doobieVersion     = "0.8.4"
lazy val circeVersion      = "0.12.2"
lazy val http4sVersion     = "0.21.0-M5"
lazy val log4catsVersion   = "1.0.1"

lazy val projectDeps = Seq(
  "org.ergoplatform"  %% "ergo-wallet"         % ergoWalletVersion,
  "org.typelevel"     %% "cats-effect"         % "2.0.0-RC2",
  "dev.zio"           %% "zio"                 % "1.0.0-RC16",
  "dev.zio"           %% "zio-interop-cats"    % "2.0.0.0-RC6",
  "co.fs2"            %% "fs2-core"            % "2.0.1",
  "org.tpolecat"      %% "doobie-core"         % doobieVersion,
  "org.tpolecat"      %% "doobie-postgres"     % doobieVersion,
  "org.tpolecat"      %% "doobie-scalatest"    % doobieVersion,
  "org.tpolecat"      %% "doobie-hikari"       % doobieVersion,
  "org.tpolecat"      %% "doobie-refined"      % doobieVersion,
  "io.circe"          %% "circe-core"          % circeVersion,
  "io.circe"          %% "circe-generic"       % circeVersion,
  "org.http4s"        %% "http4s-dsl"          % http4sVersion,
  "org.http4s"        %% "http4s-blaze-client" % http4sVersion,
  "org.http4s"        %% "http4s-blaze-server" % http4sVersion,
  "org.http4s"        %% "http4s-circe"        % http4sVersion,
  "io.chrisdavenport" %% "log4cats-core"       % log4catsVersion,
  "io.chrisdavenport" %% "log4cats-slf4j"      % log4catsVersion,
  "eu.timepit"        %% "refined"             % "0.9.10",
  "io.estatico"       %% "newtype"             % "0.4.3",
  "org.slf4j"         % "slf4j-simple"         % "1.7.28",
  "org.flywaydb"      % "flyway-core"          % "5.1.1",
)

lazy val testDeps = Seq(
  "org.tpolecat"       %% "doobie-scalatest"     % doobieVersion % Test,
  "org.scalactic"      %% "scalactic"            % "3.0.8"       % Test,
  "org.scalatest"      %% "scalatest"            % "3.0.8"       % Test,
  "org.scalacheck"     %% "scalacheck"           % "1.14.1"      % Test,
  "org.testcontainers" % "postgresql"            % "1.7.3"       % Test,
  "com.dimafeng"       %% "testcontainers-scala" % "0.18.0"      % Test
)

libraryDependencies ++= (projectDeps ++ testDeps)

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

addCompilerPlugin("org.typelevel"   % "kind-projector" % "0.11.0" cross CrossVersion.full)
addCompilerPlugin("org.scalamacros" % "paradise"       % "2.1.1" cross CrossVersion.full)
