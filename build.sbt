name := "FollowBackBot"

version := "0.1.0"

organization := "nu.glen"

scalaVersion := "2.9.2"

resolvers ++= Seq(
  "Twitter"   at "http://maven.twttr.com",
  "Twitter4j" at "http://twitter4j.org/maven2",
  "SnakeYaml" at "http://oss.sonatype.org/content/groups/public/"
)

libraryDependencies ++= Seq(
  "com.twitter"   % "util-core"        % "5.3.6",
  "com.twitter"   % "util-logging"     % "5.3.6",
  "org.twitter4j" % "twitter4j-core"   % "2.2.6",
  "org.twitter4j" % "twitter4j-stream" % "2.2.6",
  "org.yaml"      % "snakeyaml"        % "1.10",
  "org.scalatest" %% "scalatest"       % "1.8"   % "test",
  "org.mockito"   % "mockito-core"     % "1.9.0" % "test"
)

seq(com.github.retronym.SbtOneJar.oneJarSettings: _*)

scalacOptions += "-deprecation"