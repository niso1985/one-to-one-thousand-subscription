name := """one-to-1000-subscription"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.10"

libraryDependencies ++= Seq(
  jdbc,
  guice,
  "com.stripe" % "stripe-java" % "10.0.2",
  "com.typesafe" % "config" % "1.4.0",
  "org.postgresql" % "postgresql" % "42.2.10",
  "org.flywaydb" %% "flyway-play" % "4.0.0",
  "com.typesafe.play" %% "play-slick" % "3.0.2",
  ws
)
