import sbt._
import sbt.Keys._

object BuildSettings {
  val buildVersion = "0.10.6-THIB"

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
    mappings in (Compile, packageDoc) ~= filter, 
    publishTo := Some(Resolver.sftp(
      "iliaz",
      "scala.iliaz.com"
    ) as ("scala_iliaz_com", Path.userHome / ".ssh" / "id_rsa"))
  )
}

object Resolvers {
  val typesafe = Seq(
    "Typesafe repository snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
    "Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/")
  val resolversList = typesafe
}

object Dependencies {
  val netty = "io.netty" % "netty" % "3.6.5.Final" cross CrossVersion.Disabled

  val akkaActor = "com.typesafe.akka" %% "akka-actor" % "2.2.1"

  val iteratees = "com.typesafe.play" %% "play-iteratees" % "2.2.0-RC2"

  val specs = "org.specs2" %% "specs2" % "2.2.1" % "test"

  val log4jVersion = "2.0-beta9"
  val log4j = Seq("org.apache.logging.log4j" % "log4j-api" % log4jVersion, "org.apache.logging.log4j" % "log4j-core" % log4jVersion)
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
        akkaActor,
        iteratees,
        specs) ++ log4j))) dependsOn (bsonmacros)

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
