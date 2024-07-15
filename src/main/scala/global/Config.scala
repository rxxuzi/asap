package global

import com.google.gson.{Gson, GsonBuilder}

import java.io.{FileWriter, PrintWriter}
import scala.util.{Try, Using}

case class SshConfig(autoConnect: Boolean, configFile: String)
case class OutConfig(cache: Boolean, log: Boolean, dbg: Boolean)
case class DirConfig(downloadsDir: String, cacheDir: String, dbgDir: String)

class Config(
              val project: String,
              val css: Boolean,
              val ssh: SshConfig,
              val out: OutConfig,
              val dir: DirConfig
            )

object Config {
  var configPath = "asap.json"
  private lazy val instance: Config = loadConfig(configPath).getOrElse(defaultConfig)

  // デフォルト値の定義
  private val defaultConfig = new Config(
    project = "Asap",
    css = true,
    ssh = SshConfig(autoConnect = false, configFile = "ssh.json"),
    out = OutConfig(
      cache = false,
      log = true,
      dbg = false
    ),
    dir = DirConfig(
      downloadsDir = "downloads",
      cacheDir = "cache",
      dbgDir = "debug"
    )
  )

  private def loadConfig(path: String): Try[Config] = {
    val gson = new Gson()
    Using(scala.io.Source.fromFile(path)) { source =>
      gson.fromJson(source.mkString, classOf[Config])
    }.recoverWith { case e =>
      println(s"Warning: Failed to load config from $path. Using default configuration. Error: ${e.getMessage}")
      Try(defaultConfig)
    }
  }

  def project: String = instance.project
  def css : Boolean  = instance.css
  def ssh: SshConfig = instance.ssh
  def out: OutConfig = instance.out
  def dir: DirConfig = instance.dir

  def initialize(): Unit = {
    println(s"Config initialized with project: ${instance.project}")
  }

  def gen(): Unit = {
    val gson = new GsonBuilder().setPrettyPrinting().create()
    val json = gson.toJson(instance)
    Using(new PrintWriter(new FileWriter(configPath))) { writer =>
      writer.write(json)
    }.fold(
      error => println(s"Failed to save config to $configPath: ${error.getMessage}"),
      _ => println(s"Config successfully saved to $configPath")
    )
  }
}