name := """orcstorm-gallery"""

version := "0.1a"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.4"

routesGenerator := InjectedRoutesGenerator

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

libraryDependencies ++= Seq(
    "com.typesafe.play" %% "play-slick" % "3.0.3",
    "com.typesafe.play" %% "play-slick-evolutions" % "3.0.3",
    "com.typesafe.play" %% "play-json" % "2.6.0",
    "mysql" % "mysql-connector-java" % "5.1.34",
    "org.scalatestplus.play" %% "scalatestplus-play" % "3.0.0" % "test",
    "commons-io" % "commons-io" % "2.6",
    "commons-codec" % "commons-codec" % "1.13",
    specs2 % Test,
    guice,
    evolutions, 
    ws
)

resolvers += "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"
sourceDirectories in (Compile, TwirlKeys.compileTemplates) :=
  (unmanagedSourceDirectories in Compile).value

fork in run := true
