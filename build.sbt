ThisBuild / scalaVersion := "2.13.8"

ThisBuild / version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .settings(
      name := "actors-as-traders"
  )

val akkaVersion = "2.7.0"
val akkaHttpVersion = "10.5.2"

// Akka Essentials
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % akkaVersion

// Akka Streams
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-stream-typed" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion

libraryDependencies += "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion

// Logging
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.11"

// PureConfig is a Scala library for loading configuration files. It reads Typesafe Config configurations
libraryDependencies += "com.github.pureconfig" %% "pureconfig" % "0.17.8"
// reactive mongo connector
libraryDependencies += "org.reactivemongo" %% "reactivemongo-akkastream" % "1.0.10"
libraryDependencies += "org.reactivemongo" %% "reactivemongo" % "1.0.10"

