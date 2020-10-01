import sbtghpackages.TokenSource.{GitConfig,Or,Environment}

val akkaVersion = "2.6.8"
val akkaHttpVersion = "10.2.0"
val akkaHttpCirce = "1.31.0"
val circeVersion = "0.13.0"
val scalaTestVersion = "3.1.2"
val commonsCodecVersion = "1.15"

lazy val rootProject = (project in file("."))
  .configs(IntegrationTest)
  .settings(
    Defaults.itSettings,
    IntegrationTest / dependencyClasspath := (IntegrationTest / dependencyClasspath).value ++ (Test / exportedProducts).value,
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-Ymacro-annotations"),
    parallelExecution in Test := false,
    parallelExecution in IntegrationTest := false,
    name:= "psg-cardano-wallet-api",
    version := "0.1.3-SNAPSHOT",
    scalaVersion := "2.13.3",
    organization := "iog.psg",
    githubOwner := "input-output-hk",
    githubRepository := "psg-cardano-wallet-api",
    githubTokenSource := Or(GitConfig("github.token"), Environment("GITHUB_TOKEN")),
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "io.circe" %% "circe-generic-extras" % circeVersion,
      "de.heikoseeberger" %% "akka-http-circe" % akkaHttpCirce,
      "commons-codec" % "commons-codec" % commonsCodecVersion,
      "org.scalatest" %% "scalatest" % scalaTestVersion % "it, test",
    )
)

