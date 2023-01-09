ThisBuild / scalaVersion := "2.13.8"

ThisBuild / version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .settings(
      name := "ScalaHFT"
  )

val akkaVersion = "2.7.0"
val akkaHttpVersion = "10.4.0"

// Akka Essentials
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % akkaVersion

// Akka Streams
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion
// https://mvnrepository.com/artifact/com.typesafe.akka/akka-stream-typed
libraryDependencies += "com.typesafe.akka" %% "akka-stream-typed" % akkaVersion


libraryDependencies += "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion

// Logging
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.11"


// Alpakka
libraryDependencies ++= Seq(
//  "com.lightbend.akka" %% "akka-stream-alpakka-csv" % "3.0.4",
)

//val circeVersion = "0.14.1"
//libraryDependencies += "io.circe" %% "circe-parser" % circeVersion