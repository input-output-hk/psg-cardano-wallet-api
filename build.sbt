val akkaVersion = "2.6.19"
val akkaHttpVersion = "10.2.9"
val akkaHttpCirce = "1.39.2"
val akkaJsonStreaming = "3.0.4"
val circeVersion = "0.14.2"
val scalaTestVersion = "3.2.11"
val commonsCodecVersion = "1.15"

lazy val rootProject = (project in file("."))
  .configs(IntegrationTest)
  .settings(
    Defaults.itSettings,
    IntegrationTest / dependencyClasspath := (IntegrationTest / dependencyClasspath).value ++ (Test / exportedProducts).value,
    name:= "psg-cardano-wallet-api",
    scalaVersion := "2.13.3",
    organization := "iog.psg",
    homepage := Some(url("https://github.com/input-output-hk/psg-cardano-wallet-api")),
    scmInfo := Some(ScmInfo(url("https://github.com/input-output-hk/psg-cardano-wallet-api"), "scm:git@github.com:input-output-hk/psg-cardano-wallet-api.git")),
    developers := List(
      Developer("mcsherrylabs", "Alan McSherry", "alan.mcsherry@iohk.io", url("https://github.com/mcsherrylabs")),
      Developer("maciejbak85", "Maciej Bak", "maciej.bak@iohk.io", url("https://github.com/maciejbak85"))
    ),
    publishMavenStyle := true,
    updateOptions := updateOptions.value.withGigahorse(false),
    licenses := Seq("APL2" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt")),
    description := "A java/scala wrapper for the cardano wallet backend API",
    usePgpKeyHex("75E12F006A3F08C757EE8343927AE95EEEF4A02F"),
    isSnapshot := false,
    publishTo := Some {
      // publish to the sonatype repository
      val sonaUrl = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        "snapshots" at sonaUrl + "content/repositories/snapshots"
      else
        "releases" at sonaUrl + "service/local/staging/deploy/maven2"
    },
    credentials += Credentials("Sonatype Nexus Repository Manager",
    "oss.sonatype.org",
    sys.env.getOrElse("SONA_USER", ""),
    sys.env.getOrElse("SONA_PASS", "")),
    ThisBuild / dynverSonatypeSnapshots  := true,
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-Ymacro-annotations"),
    Test / parallelExecution := true,
    IntegrationTest / parallelExecution := false,
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.lightbend.akka" %% "akka-stream-alpakka-json-streaming" % akkaJsonStreaming,
      "io.circe" %% "circe-generic-extras" % circeVersion,
      "de.heikoseeberger" %% "akka-http-circe" % akkaHttpCirce,
      "commons-codec" % "commons-codec" % commonsCodecVersion,
      "org.scalatest" %% "scalatest" % scalaTestVersion % "it, test",
    ),
    assembly / assemblyMergeStrategy := {
      case "module-info.class" => MergeStrategy.discard
      case x =>
        val oldStrategy = (assembly / assemblyMergeStrategy).value
        oldStrategy(x)
    }
)