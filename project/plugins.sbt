addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.10.0-RC1")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.9")
addCompilerPlugin("org.typelevel"   % "kind-projector" % "0.11.0" cross CrossVersion.full)
addCompilerPlugin("org.scalamacros" % "paradise"       % "2.1.1" cross CrossVersion.full)

