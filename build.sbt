ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.12"

lazy val root = (project in file("."))
  .settings(
    name := "ml-scala",
  )

libraryDependencies += "com.github.haifengl" %% "smile-scala" % "3.0.1"
libraryDependencies += "io.github.cibotech" %% "evilplot" % "0.9.0"
libraryDependencies += "io.github.cibotech" %% "evilplot-repl" % "0.9.0"
libraryDependencies += "org.typelevel" %% "cats-effect" % "3.5.1"
libraryDependencies += "com.typesafe" % "config" % "1.4.2"



addCompilerPlugin("org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full)
