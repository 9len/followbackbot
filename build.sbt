import com.typesafe.startscript.StartScriptPlugin

seq(StartScriptPlugin.startScriptForClassesSettings: _*)

name := "FollowBackBot"

version := "0.1.0-SNAPSHOT"

organization := "nu.glen"

scalaVersion := "2.9.2"

resolvers ++= Seq(
  "Twitter"   at "http://maven.twttr.com",
  "Twitter4j" at "http://twitter4j.org/maven2"
)

libraryDependencies ++= Seq(
  "com.google.code.findbugs" % "jsr305"           % "2.0.1",
  "com.google.guava"         % "guava"            % "13.0",
  "com.twitter"              % "util-core"        % "5.3.6",
  "com.twitter"              % "util-logging"     % "5.3.6",
  "org.twitter4j"            % "twitter4j-core"   % "3.0.3",
  "org.twitter4j"            % "twitter4j-stream" % "3.0.3",
  "org.scalatest"            %% "scalatest"       % "1.8"   % "test",
  "org.mockito"              % "mockito-core"     % "1.9.0" % "test"
)

scalacOptions += "-deprecation"
