ThisBuild / version := "0.2.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.1"

// Path to JavaFX SDK
lazy val javaFXHome = sys.env("JAVAFX_HOME")

lazy val root = (project in file("."))
  .settings(
    name := "Asap",
    libraryDependencies ++= Seq(
      "org.scalafx" %% "scalafx" % "20.0.0-R31",
      "com.google.code.gson" % "gson" % "2.10.1",
    )
  )

libraryDependencies ++= Seq(
  "org.apache.sshd" % "sshd-core" % "2.13.1",
  "org.apache.sshd" % "sshd-sftp" % "2.13.1",
  "org.apache.sshd" % "sshd-common" % "2.12.0",
  "org.apache.sshd" % "sshd-putty" % "2.12.1",
  "org.bouncycastle" % "bcpkix-jdk15on" % "1.70",
  "org.slf4j" % "slf4j-api" % "2.0.12",
  "org.slf4j" % "slf4j-simple" % "2.0.13",
)

libraryDependencies ++= Seq(
  "org.openjfx" % "javafx-base" % "22.0.1",
  "org.openjfx" % "javafx-controls" % "22.0.1",
  "org.openjfx" % "javafx-web" % "22.0.1",
  "org.openjfx" % "javafx-fxml" % "22.0.1"
)

fork := true
run / fork := true

// Remove Java VM options from scalacOptions
scalacOptions ++= Seq(
  s"-Djava.library.path=$javaFXHome\\lib"
)

run / javaOptions ++= Seq(
  s"-Djava.library.path=$javaFXHome\\lib",
  s"--module-path=$javaFXHome\\lib",
  "--add-modules", "javafx.controls,javafx.web,javafx.fxml,javafx.base",
)

// Ensure that the JavaFX libraries are copied to the lib directory
Compile / unmanagedJars ++= {
  val libDir = file(s"$javaFXHome\\lib")
  (libDir ** "*.jar").classpath
}
