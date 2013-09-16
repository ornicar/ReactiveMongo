organization := "org.reactivemongo"

name := "ReactiveMongo-BSON"

version := "0.10.4-THIB"

scalaVersion := "2.10.2"

//scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xlog-implicits", "-Yinfer-debug")

// scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xlog-implicits", "-Yinfer-debug", "-Xprint:typer")

scalacOptions ++= Seq("-unchecked", "-deprecation")

libraryDependencies += "org.specs2" %% "specs2" % "1.13" % "test"

scalacOptions in (Compile, doc) ++= Opts.doc.title("ReactiveMongo-BSON API")

scalacOptions in (Compile, doc) ++= Opts.doc.version("0.1-SNAPSHOT")
