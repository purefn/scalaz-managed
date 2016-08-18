name := "scalaz-managed"

organization := "org.scalaz"

scalaVersion := "2.11.8"

crossScalaVersions := List("2.10.6", "2.11.8")

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.6.3")

scalacOptions ++=
  (List
    ( "-deprecation"
    , "-encoding", "UTF-8"       // yes, this is 2 args
    , "-feature"
    , "-language:existentials"
    , "-language:higherKinds"
    , "-language:implicitConversions"
    , "-unchecked"
    , "-Xfatal-warnings"
    , "-Xlint"
    , "-Yno-adapted-args"
    , "-Ywarn-dead-code"        // N.B. doesn't work well with the ??? hole
    , "-Ywarn-numeric-widen"
    , "-Ywarn-value-discard"
    , "-Xfuture"
    )
  )

scalacOptions ++= {
  if (scalaVersion.value.startsWith("2.10")) Nil
  else List("-Ywarn-unused-import")
}

scalacOptions in console := Seq()
scalacOptions in consoleQuick := Seq()

scalacOptions in Test ++= Seq("-Yrangepos")

libraryDependencies ++=
  (List
    ( "org.scalaz" %% "scalaz-core"  % "7.2.5"
    , "org.scalaz" %% "scalaz-effect"  % "7.2.5"
    )
  )

publishMavenStyle := true

licenses += ("BSD", url("https://opensource.org/licenses/BSD-3-Clause"))
