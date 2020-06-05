name := "OBP-CBS"

version := "0.1"

scalaVersion := "2.13.2"

libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick" % "3.3.2",
  "org.slf4j" % "slf4j-nop" % "1.7.26",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.3.2",
  "org.postgresql" % "postgresql" % "42.2.12",
  "com.chuusai" %% "shapeless" % "2.3.3",
  "io.underscore" %% "slickless" % "0.3.6"
)

lazy val commonSettings = Seq(
  version := "0.1",
  organization := "com.tesobe",
  scalaVersion := "2.13.2"
)

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % "2.0.0-M1",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2",
  "javax.xml.bind" % "jaxb-api" % "2.3.1"
)

lazy val root = (project in file(".")).enablePlugins(ScalaxbPlugin)