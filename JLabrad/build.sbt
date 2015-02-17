organization := "org.labrad"

name := "jlabrad"

version := "0.2.0-M1"

licenses += ("GPL-2.0", url("http://www.gnu.org/licenses/gpl-2.0.html"))

crossPaths := false // don't add scala version suffix to jars

libraryDependencies ++= Seq(
  "com.google.guava" % "guava" % "18.0" withSources(),
  "org.jboss.netty" % "netty" % "3.2.10.Final" withSources()
)


// enable publishing artifacts to bintray
bintraySettings
