
name := "FollowBackBot"

version := "0.1.0-SNAPSHOT"

organization := "nu.glen"

scalaVersion := "2.10.4"

resolvers ++= Seq(
  "Twitter" at "http://maven.twttr.com",
  "Twitter4j" at "http://twitter4j.org/maven2"
)

libraryDependencies ++= Seq(
  "com.google.code.findbugs" % "jsr305" % "2.0.1",
  "com.google.guava" % "guava" % "13.0",
  "org.twitter4j" % "twitter4j-core" % "3.0.5",
  "org.twitter4j" % "twitter4j-stream" % "3.0.5",
  "ch.qos.logback" % "logback-classic" % "1.1.2",
  "com.typesafe" %% "scalalogging-slf4j" % "1.1.0",
  "org.scalatest" %% "scalatest" % "2.1.5" % "test",
  "org.mockito" % "mockito-core" % "1.9.0" % "test"
)

scalacOptions += "-deprecation"
