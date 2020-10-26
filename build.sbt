name := "OBP-CBS"

version := "0.1"

scalaVersion := "2.12.10"

libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick" % "3.3.2",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.3.2",
  "org.postgresql" % "postgresql" % "42.2.12",
  "com.chuusai" %% "shapeless" % "2.3.3",
  "io.underscore" %% "slickless" % "0.3.6"
)

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % "2.0.0-M1",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2",
  "javax.xml.bind" % "jaxb-api" % "2.3.1"
)

/*
lazy val root = (project in file("."))
  .enablePlugins(ScalaxbPlugin)
  .settings(scalaxbPackageName in(Compile, scalaxb) := "sepa.sct.generated.inquiryClaimValueDateCorrectionNegativeAnswer")
*/

lazy val akkaVersion = "2.5.19"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "net.liftweb" %% "lift-common" % "3.4.1",
  "com.twitter" %% "chill-akka" % "0.9.1"
)

val circeVersion = "0.12.3"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % "10.1.12",
  "com.typesafe.akka" %% "akka-stream" % "2.5.26"
)

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.2.0" % "test"
)