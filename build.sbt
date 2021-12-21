val scala3Version = "3.1.0"
val akkaVersion      = "2.6.18"

lazy val root = project
  .in(file("."))
  .settings(
    name := "Test",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor"               % akkaVersion,
      "com.typesafe.akka" %% "akka-actor-typed"         % akkaVersion
    )
  )