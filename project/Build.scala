import sbt._
import Keys._

import android.Keys._

object General {
  val settings = Defaults.defaultSettings ++ Seq (
    name := "Fedora Mobile",
    version := "0.1",
    scalaVersion := "2.11.3-typelevel",
    resolvers             ++= Seq(
      "relrod @ FedoraPeople" at "https://codeblock.fedorapeople.org/maven/",
      "sonatype-s" at "http://oss.sonatype.org/content/repositories/snapshots",
      Resolver.mavenLocal
    ),
    libraryDependencies   ++= Seq(
      //"me.elrod" %% "pkgwat" % "1.0.0",
      "org.scalaz" %% "scalaz-core" % "7.1.0",
      "org.scalaz" %% "scalaz-concurrent" % "7.1.0",
      "io.argonaut" %% "argonaut" % "6.1-M4",
      "com.github.chrisbanes.actionbarpulltorefresh" % "library" % "0.5",
      "org.scalacheck" %% "scalacheck" % "1.11.0" % "test"
    ),
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-feature",
      "-language:existentials",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-optimise",
      "-target:jvm-1.6",
      "-unchecked",
      "-Xfatal-warnings",
      "-Xlint",
      "-Yno-adapted-args",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen",
      "-Ywarn-value-discard"
    ),
    javacOptions          ++= Seq(
      "-encoding", "utf8",
      "-source", "1.6",
      "-target", "1.6",
      "-Xlint:deprecation",
      "-Xlint:unchecked"
    )
  )

  val proguardSettings = Seq (
    useProguard in Android := true,
    proguardOptions in Android += "-dontwarn com.google.android.gms.**",
    proguardOptions in Android += "-dontwarn com.google.appengine.api.ThreadManager",
    proguardOptions in Android += "-dontwarn com.google.apphosting.api.ApiProxy",
    proguardOptions in Android += "-dontwarn javax.annotation.**",
    proguardOptions in Android += "-dontwarn javax.inject.**",
    proguardOptions in Android += "-dontwarn scalaz.concurrent.*",
    proguardOptions in Android += "-keep class com.google.android.gms.** { *; }",
    proguardOptions in Android += "-keep class org.parboiled.matchervisitors.MatcherVisitor",
    proguardOptions in Android += "-keep class scala.Function1",
    proguardOptions in Android += "-keep class scala.PartialFunction",
    proguardOptions in Android += "-keep class scala.util.parsing.combinator.Parsers",
    proguardOptions in Android += "-dontwarn scala.collection.concurrent.RestartException",

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
