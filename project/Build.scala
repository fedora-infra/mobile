import sbt._
import Keys._
import AndroidKeys._

import scalariform.formatter.preferences._
import com.typesafe.sbt.SbtScalariform._

object General {
  val settings = Defaults.defaultSettings ++ Seq (
    name := "Fedora Mobile",
    version := "0.1",
    versionCode := 0,
    scalaVersion := "2.10.1",
    platformName in Android := "android-17",
    resolvers             ++= Seq(
      "spray" at "http://repo.spray.io/",
      "relrod @ FedoraPeople" at "http://codeblock.fedorapeople.org/maven/"
    ),
    libraryDependencies   ++= Seq(
      "io.spray" %% "spray-json" % "1.2.5",
      //"me.elrod" %% "pkgwat" % "1.0.0",
      "org.scalatest" %% "scalatest" % "1.9.1" % "test"
    ),
    scalacOptions         := Seq(
      "-encoding", "utf8",
      "-target:jvm-1.6"
    ),
    javacOptions          ++= Seq(
      "-encoding", "utf8",
      "-source", "1.6",
      "-target", "1.6"
    )
  ) ++
  defaultScalariformSettings ++ Seq(
    ScalariformKeys.preferences := FormattingPreferences().
    setPreference(PreserveDanglingCloseParenthesis, true).
    setPreference(MultilineScaladocCommentsStartOnFirstLine, true).
    setPreference(PlaceScaladocAsterisksBeneathSecondAsterisk, true)
  )

  val proguardSettings = Seq (
    useProguard in Android := true,
    proguardOption in Android := """
      -keep class scala.Function1
      -keep class scala.collection.SeqLike { public protected *; }
      -keep class spray.json.*
    """
  )

  lazy val fullAndroidSettings =
    General.settings ++
    AndroidProject.androidSettings ++
    TypedResources.settings ++
    proguardSettings ++
    AndroidManifestGenerator.settings ++
    AndroidMarketPublish.settings ++ Seq (
      keyalias in Android := "change-me"
    )
}

object AndroidBuild extends Build {
  lazy val main = Project (
    "FedoraMobile",
    file("."),
    settings = General.fullAndroidSettings
  )

  lazy val tests = Project (
    "tests",
    file("tests"),
    settings = General.settings ++
               AndroidTest.androidSettings ++
               General.proguardSettings ++ Seq (
      name := "Fedora MobileTests"
    )
  ) dependsOn main
}
