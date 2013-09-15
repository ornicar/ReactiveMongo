import sbt._
import sbt.Keys._

object BuildSettings {
  val buildVersion = "0.10.0-THIB"

  val filter = { (ms: Seq[(File, String)]) =>
    ms filter {
      case (file, path) =>
        path != "logback.xml" && !path.startsWith("toignore") && !path.startsWith("samples")
    }
  }

  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "org.reactivemongo",
    version := buildVersion,
    scalaVersion := "2.10.2",
    javaOptions in test ++= Seq("-Xmx512m", "-XX:MaxPermSize=512m"),
    scalacOptions ++= Seq("-unchecked", "-deprecation" /*, "-Xlog-implicits", "-Yinfer-debug", "-Xprint:typer", "-Yinfer-debug", "-Xlog-implicits", "-Xprint:typer"*/ ),
    scalacOptions in (Compile, doc) ++= Seq("-unchecked", "-deprecation", "-implicits"),
    mappings in (Compile, packageBin) ~= filter,
    mappings in (Compile, packageSrc) ~= filter,
    mappings in (Compile, packageDoc) ~= filter) 
}

object Resolvers {
  val typesafe = Seq(
    "Typesafe repository snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
    "Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/")
  val resolversList = typesafe
}

object Dependencies {
  val netty = "io.netty" % "netty" % "3.6.5.Final" cross CrossVersion.Disabled

  def akkaActor(sv: String) = sv match {
    //case "2.10.0" => "com.typesafe.akka" %% "akka-actor" % "2.1.0"
    //case "2.10.1" => "com.typesafe.akka" %% "akka-actor" % "2.1.2"
    case "2.10.2" => "com.typesafe.akka" %% "akka-actor" % "2.2.1"
  }

  def iteratees(sv: String) = sv match {
    //case "2.10.0" => "play" %% "play-iteratees" % "2.1.0"
    //case "2.10.1" => "play" %% "play-iteratees" % "2.1.1"
    case "2.10.2" => "com.typesafe.play" %% "play-iteratees" % "2.2.0-RC1"
  }

  val logbackVer = "1.0.11"
  val logback = Seq(
    "ch.qos.logback" % "logback-core" % logbackVer,
    "ch.qos.logback" % "logback-classic" % logbackVer)

  def specs(sv: String) = sv match {
    //case "2.10.0" => "org.specs2" % "specs2" % "1.14" % "test" cross CrossVersion.binary
    //case "2.10.1" => "org.specs2" % "specs2" % "1.14" % "test" cross CrossVersion.binary
    case "2.10.2" => "org.specs2" % "specs2" % "1.14" % "test" cross CrossVersion.binary
  }

  val junit = "junit" % "junit" % "4.8" % "test" cross CrossVersion.Disabled
  val testDeps = Seq(junit)
}

object ReactiveMongoBuild extends Build {
  import BuildSettings._
  import Resolvers._
  import Dependencies._

  lazy val reactivemongo = Project(
    "ReactiveMongo-Root",
    file("."),
    settings = buildSettings ++ Unidoc.settings ++ Seq(
      publish := {}
    )) aggregate(driver, bson, bsonmacros)

  lazy val driver = Project(
    "ReactiveMongo",
    file("driver"),
    settings = buildSettings ++ Seq(
      resolvers := resolversList,
      libraryDependencies <++= (scalaVersion)(sv => Seq(
        netty,
        akkaActor(sv),
        iteratees(sv),
        specs(sv)) ++ logback ++ testDeps))) dependsOn (bsonmacros)

  lazy val bson = Project(
    "ReactiveMongo-BSON",
    file("bson"),
    settings = buildSettings)

  lazy val bsonmacros = Project(
    "ReactiveMongo-BSON-Macros",
    file("macros"),
    settings = buildSettings ++ Seq(
      libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-compiler" % _)
    )) dependsOn (bson)
}
