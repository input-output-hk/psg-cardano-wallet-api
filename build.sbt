
scalaVersion := "2.13.3"

val circeVersion = "0.13.0"
val akkaVersion = "2.6.8"
val akkaHttpVersion = "10.2.0"

libraryDependencies ++=  Seq(
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion
)

javacOptions ++= Seq("-source", "11", "-target", "11")


scalacOptions ++= Seq("-unchecked", "-deprecation")
