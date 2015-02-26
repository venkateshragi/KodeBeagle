name := "BetterDocs"

version := "1.0"

scalaVersion := "2.11.5"

libraryDependencies += "org.apache.spark" %% "spark-core" % "1.2.1"

// transitively uses commons-lang3-3.3.2
// commons-httpclient-3.1
// commons-io-2.4
// json4s-jackson_2.11-3.2.10
// json4s-ast_2.11-3.2.10.jar
// commons-compress-1.4.1