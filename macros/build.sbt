organization := "org.reactivemongo"

name := "ReactiveMongo-BSON-Macros"

version := "0.10.7-THIB"

scalaVersion := "2.10.2"

scalacOptions ++= Seq("-unchecked", "-deprecation")

libraryDependencies += "org.specs2" %% "specs2" % "2.2.1" % "test"

scalacOptions in (Compile, doc) ++= Opts.doc.title("ReactiveMongo-BSON-Macros API")

scalacOptions in (Compile, doc) ++= Opts.doc.version("0.9-SNAPSHOT")
