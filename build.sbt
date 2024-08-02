ThisBuild / version := "1.6"
ThisBuild / scalaVersion := "3.3.1"

// Path to JavaFX SDK
lazy val javaFXHome = sys.env("JAVAFX_HOME")

val osName = settingKey[String]("Name of the operating system")
osName := (System.getProperty("os.name") match {
  case name if name.startsWith("Linux") => "linux"
  case name if name.startsWith("Mac") => "mac"
  case name if name.startsWith("Windows") => "win"
  case _ => throw new Exception("Unknown platform!")
})

lazy val root = (project in file("."))
  .settings(
    name := "Asap",
    libraryDependencies ++= Seq(
      "com.google.code.gson" % "gson" % "2.10.1",
      "org.apache.sshd" % "sshd-core" % "2.13.1",
      "org.apache.sshd" % "sshd-sftp" % "2.13.1",
      "org.apache.sshd" % "sshd-common" % "2.12.0",
      "org.apache.sshd" % "sshd-putty" % "2.12.1",
      "org.bouncycastle" % "bcpkix-jdk15on" % "1.70",
      "org.slf4j" % "slf4j-api" % "2.0.12",
      "org.slf4j" % "slf4j-simple" % "2.0.13",
      "org.openjfx" % "javafx-base" % "22.0.1" classifier osName.value,
      "org.openjfx" % "javafx-controls" % "22.0.1" classifier osName.value,
      "org.openjfx" % "javafx-web" % "22.0.1" classifier osName.value,
      "org.openjfx" % "javafx-fxml" % "22.0.1" classifier osName.value
    ),

    fork := true,
    run / fork := true,

    scalacOptions ++= Seq(
      s"-Djava.library.path=$javaFXHome/lib"
    ),

    run / javaOptions ++= Seq(
      s"-Djava.library.path=$javaFXHome/lib",
      s"--module-path=$javaFXHome/lib",
      "--add-modules", "javafx.controls,javafx.web,javafx.fxml,javafx.base",
    ),
  )

// Assembly settings
assembly / mainClass := Some("Main")
assembly / assemblyJarName := "Asap.jar"

assembly / assemblyMergeStrategy := {
  case x if x.endsWith("module-info.class") => MergeStrategy.discard
  case x if x.endsWith(".SF") => MergeStrategy.discard
  case x if x.endsWith(".DSA") => MergeStrategy.discard
  case x if x.endsWith(".RSA") => MergeStrategy.discard
  case PathList("META-INF", xs @ _*) =>
    xs map {_.toLowerCase} match {
      case "manifest.mf" :: Nil | "index.list" :: Nil | "dependencies" :: Nil =>
        MergeStrategy.discard
      case _ => MergeStrategy.first
    }
  case _ => MergeStrategy.first
}

assembly / assemblyExcludedJars := {
  val cp = (assembly / fullClasspath).value
  cp filter { _.data.getName.startsWith("javafx") }
}

// Include JavaFX JARs in the assembled JAR
Compile / unmanagedJars ++= {
  val libDir = file(s"$javaFXHome/lib")
  (libDir ** "*.jar").classpath
}

assembly / assemblyOption := (assembly / assemblyOption).value.withIncludeScala(false).withIncludeDependency(true)

Compile / packageBin / mainClass := Some("Main")
