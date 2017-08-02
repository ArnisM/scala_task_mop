name := "Challenge"

version := "1.0"

scalaVersion := "2.12.2"
name := "task-news"

assemblyJarName in assembly := "task_01.jar"

libraryDependencies ++= {
  Seq(

    "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.7",
    "joda-time" % "joda-time" % "2.9.9",
    "net.debasishg" %% "redisclient" % "3.4",
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
    "net.debasishg" %% "redisclient" % "3.4",
    "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
    "com.typesafe.slick" %% "slick" % "3.2.0",
    "com.rabbitmq" % "amqp-client"  % "4.1.0",
    "org.scala-lang" % "scala-reflect" % "2.12.2",
    "com.typesafe" % "config" % "1.2.1",
    "net.debasishg" %% "redisclient" % "3.4",
    "com.google.code.gson" % "gson" % "2.8.0",
    "org.scalaj" % "scalaj-http_2.11" % "2.3.0"

  )
}