val akkaVersion = "2.6.8"
val akkaHttpVersion = "10.2.0"
val akkaHttpCirce = "1.31.0"
val akkaJsonStreaming = "2.0.2"
val circeVersion = "0.13.0"
val scalaTestVersion = "3.1.2"
val commonsCodecVersion = "1.15"

lazy val rootProject = (project in file("."))
  .configs(IntegrationTest)
  .settings(
    Defaults.itSettings,
    IntegrationTest / dependencyClasspath := (IntegrationTest / dependencyClasspath).value ++ (Test / exportedProducts).value,
    name:= "psg-cardano-wallet-api",
    version:= "0.2.4.1",
    scalaVersion := "2.13.3",
    organization := "solutions.iog",
    homepage := Some(url("https://github.com/input-output-hk/psg-cardano-wallet-api")),
    scmInfo := Some(ScmInfo(url("https://github.com/input-output-hk/psg-cardano-wallet-api"), "scm:git@github.com:input-output-hk/psg-cardano-wallet-api.git")),
    developers := List(
      Developer("mcsherrylabs", "Alan McSherry", "alan.mcsherry@iohk.io", url("https://github.com/mcsherrylabs")),
      Developer("maciejbak85", "Maciej Bak", "maciej.bak@iohk.io", url("https://github.com/maciejbak85"))
    ),
    publishMavenStyle := true,
    licenses := Seq("APL2" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt")),
    description := "A java/scala wrapper for the cardano wallet backend API",
    usePgpKeyHex("75E12F006A3F08C757EE8343927AE95EEEF4A02F"),
    isSnapshot := false,
    publishTo := {
      val nexus = "https://nexus.mcsherrylabs.com/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "repository/snapshots")
      else
        Some("releases"  at nexus + "repository/releases")
    },
    credentials += sys.env.get("NEXUS_USER").map(userName => Credentials(
      "Sonatype Nexus Repository Manager",
      "nexus.mcsherrylabs.com",
      userName,
      sys.env.getOrElse("NEXUS_PASS", ""))
    ).getOrElse(
      Credentials(Path.userHome / ".ivy2" / ".credentials")
    ),
    //dynverSonatypeSnapshots in ThisBuild := true,
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-Ymacro-annotations"),
    parallelExecution in Test := true,
    parallelExecution in IntegrationTest := false,
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
    assemblyMergeStrategy in assembly := {
      case "module-info.class" => MergeStrategy.discard
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    }
)