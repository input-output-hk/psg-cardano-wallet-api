import sbtghpackages.TokenSource.{GitConfig,Or,Environment}

name:= "psg-cardano-wallet-api"

scalaVersion := "2.13.3"

organization := "solutions.iog.psg"

homepage := Some(url("https://github.com/input-output-hk/psg-cardano-wallet-api"))
scmInfo := Some(ScmInfo(url("https://github.com/input-output-hk/psg-cardano-wallet-api"), "git@github.com:input-output-hk/psg-cardano-wallet-api.git"))
developers := List(
  Developer("mcsherrylabs", "Alan McSherry", "alan.mcsherry@iohk.io", url("https://github.com/mcsherrylabs")),
  Developer("maciejbak85", "Maciej Bak", "maciej.bak@iohk.io", url("https://github.com/maciejbak85"))
)
publishMavenStyle := true
licenses := Seq("APL2" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt"))
description := "A java/scala wrapper for the cardano wallet backend API"

pgpPassphrase := sys.env.get("PGP_PASSPHRASE").map(_.toArray)

// publish to the sonatype repository
publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)

//githubOwner := "input-output-hk"

//githubRepository := "psg-cardano-wallet-api"

githubTokenSource := Or(GitConfig("github.token"), Environment("GITHUB_TOKEN"))

val akkaVersion = "2.6.8"
val akkaHttpVersion = "10.2.0"
val akkaHttpCirce = "1.31.0"
val circeVersion = "0.13.0"
val scalaTestVersion = "3.1.2"
val commonsCodecVersion = "1.15"

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
  "commons-codec" % "commons-codec" % commonsCodecVersion,
  "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
)


javacOptions ++= Seq("-source", "1.8", "-target", "1.8")

scalacOptions ++= Seq("-unchecked", "-deprecation", "-Ymacro-annotations")

parallelExecution in Test := false

