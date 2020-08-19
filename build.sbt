import sbtghpackages.TokenSource.{GitConfig,Or,Environment}

version := "0.1.1-SNAPSHOT"

scalaVersion := "2.13.3"

organization := "iog.psg"

githubOwner := "input-output-hk"

githubRepository := "psg-cardano-wallet-api"

githubTokenSource := Or(GitConfig("github.token"), Environment("GITHUB_TOKEN"))

val akkaVersion = "2.6.8"
val akkaHttpVersion = "10.2.0"
val akkaHttpCirce = "1.31.0"
val circeVersion = "0.13.0"

/**
 * Don't include a logger binding as this is a library for embedding
 * http://www.slf4j.org/codes.html#StaticLoggerBinder
 */
libraryDependencies ++=  Seq(
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "io.circe" %% "circe-generic-extras" % circeVersion,
  "de.heikoseeberger" %% "akka-http-circe" % akkaHttpCirce,
  "org.scalatest" %% "scalatest" % "3.1.2" % Test
)

javacOptions ++= Seq("-source", "11", "-target", "11")

scalacOptions ++= Seq("-unchecked", "-deprecation", "-Ymacro-annotations")


