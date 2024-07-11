ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.1"

lazy val root = (project in file("."))
  .settings(
    name := "Asap",
    libraryDependencies ++= Seq(
      "org.scalafx" %% "scalafx" % "20.0.0-R31",
      "com.google.code.gson" % "gson" % "2.10.1",
      "com.jcraft" % "jsch" % "0.1.55"
    )
  )

libraryDependencies ++= Seq(
  "org.openjfx" % "javafx-base" % "22.0.1",
  "org.openjfx" % "javafx-controls" % "22.0.1",
  "org.openjfx" % "javafx-web" % "22.0.1",
  "org.openjfx" % "javafx-fxml" % "22.0.1"
)

// Path to JavaFX SDK
lazy val javaFXHome = sys.env("JAVAFX_HOME")

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