name := "SparkTutorial"

version := "1.0"

libraryDependencies += "org.apache.spark" % "spark-core_2.10" % "1.0.2" withSources() withJavadoc()

libraryDependencies += "org.apache.spark" % "spark-streaming_2.10" % "1.0.2" withSources() withJavadoc()

resolvers +="Akka Repository" at "http://repo.akka.io/releases/"

libraryDependencies += "org.apache.hadoop" % "hadoop-client" % "2.2.0" withJavadoc()

libraryDependencies += "org.apache.spark" %% "spark-mllib" % "1.0.2" withSources() withJavadoc()

libraryDependencies += "io.netty" % "netty-all" % "4.0.17.Final" withSources() withJavadoc()

libraryDependencies += "com.twitter" % "util-core_2.10" % "6.22.0" withSources() withJavadoc()

