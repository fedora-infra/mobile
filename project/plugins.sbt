resolvers += Resolver.url("scalasbt releases", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns)

addSbtPlugin("org.scala-sbt" % "sbt-android" % "0.6.4")

addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.0.1")
