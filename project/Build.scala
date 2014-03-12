import sbt._
import Keys._

import android.Keys._

import scalariform.formatter.preferences._
import com.typesafe.sbt.SbtScalariform._

object General {
  val settings = Defaults.defaultSettings ++ Seq (
    name := "Fedora Mobile",
    version := "0.1",
    scalaVersion := "2.10.3",
    resolvers             ++= Seq(
      "relrod @ FedoraPeople" at "http://codeblock.fedorapeople.org/maven/",
      "sonatype-s" at "http://oss.sonatype.org/content/repositories/snapshots"
    ),
    libraryDependencies   ++= Seq(
      //"me.elrod" %% "pkgwat" % "1.0.0",
      "com.google.guava" % "guava" % "14.0.1",
      "org.scalaz" %% "scalaz-core" % "7.0.4",
      //"com.github.xuwei-k" %% "iarray" % "0.2.6",
      "org.scalaz.stream" %% "scalaz-stream" % "0.2-SNAPSHOT",
      "io.argonaut" %% "argonaut" % "6.0.1",
      "com.github.chrisbanes.actionbarpulltorefresh" % "library" % "0.5",
      "org.scalacheck" %% "scalacheck" % "1.11.0" % "test"
    ),
    scalacOptions         := Seq(
      "-encoding", "utf8",
      "-target:jvm-1.6",
      "-feature",
      "-unchecked",
      "-deprecation",
      "-optimise",
      "-Ywarn-value-discard"
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
    proguardOptions in Android += "-dontwarn com.google.android.gms.**",
    proguardOptions in Android += "-dontwarn com.google.appengine.api.ThreadManager",
    proguardOptions in Android += "-dontwarn com.google.apphosting.api.ApiProxy",
    proguardOptions in Android += "-dontwarn javax.annotation.**",
    proguardOptions in Android += "-dontwarn javax.inject.**",
    proguardOptions in Android += "-dontwarn scalaz.concurrent.*",
    proguardOptions in Android += "-dontwarn sun.misc.Unsafe",
    proguardOptions in Android += "-keep class com.google.android.gms.** { *; }",
    proguardOptions in Android += "-keep class org.parboiled.matchervisitors.MatcherVisitor",
    proguardOptions in Android += "-keep class scala.Function1",
    proguardOptions in Android += "-keep class scala.PartialFunction",
    proguardOptions in Android += "-keep class scala.util.parsing.combinator.Parsers",

    //proguardCache in Android += ProguardCache("guava") % "com.google.guava",
    proguardCache in Android += ProguardCache("scalaz") % "org.scalaz",
    proguardCache in Android += ProguardCache("argonaut") % "io.argonaut"
  )

  lazy val fullAndroidSettings =
    General.settings ++
    android.Plugin.androidBuild ++
    proguardSettings
}

object AndroidBuild extends Build {
  lazy val main = Project (
    "FedoraMobile",
    file("."),
    settings = General.fullAndroidSettings ++ Seq(
      platformTarget in Android := "android-17"
    )
  )

  lazy val tests = Project (
    "tests",
    file("tests"),
    settings = General.settings
  ) dependsOn main
}
