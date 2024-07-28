package global

import com.google.gson.{Gson, GsonBuilder}

import java.io.{FileWriter, PrintWriter}
import scala.util.{Try, Using}

case class SshConfig(autoConnect: Boolean, configFile: String, keepAlive: Boolean)
case class OutConfig(cache: Boolean, log: Boolean, dbg: Boolean)
case class DirConfig(downloadsDir: String, cacheDir: String, logDir:String, dbgDir: String)
case class GenConfig(zipper: Boolean, encrypted: Boolean)

class Config(
              val project: String,
              val css: Boolean,
              var dark: Boolean,
              val ssh: SshConfig,
              val out: OutConfig,
              val dir: DirConfig,
              val gen: GenConfig
            )

object Config {
  var configPath = "asap.json"
  val defaultLogDir = "log"
  val iconPath = "/png/icon.png"

  private lazy val instance: Config = loadConfig(configPath).getOrElse(defaultConfig)

  // デフォルト値の定義
  private val defaultConfig = new Config(
    project = "Asap",
    css = true,
    dark = true,
    ssh = SshConfig(
      autoConnect = false, 
      configFile = "ssh.json", 
      keepAlive = false
    ),
    out = OutConfig(
      cache = false,
      log = true,
      dbg = false
    ),
    dir = DirConfig(
      downloadsDir = "downloads",
      cacheDir = "cache",
      logDir = "log",
      dbgDir = "debug"
    ),
    gen = GenConfig(
      false, false
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
  def dark : Boolean = instance.dark
  def ssh: SshConfig = instance.ssh
  def out: OutConfig = instance.out
  def dir: DirConfig = instance.dir
  def gen: GenConfig = instance.gen

  def initialize(): Unit = {
    println(s"Config initialized with project: ${instance.project}")
  }
  
  def setDark(bool : Boolean) : Unit = instance.dark = bool

  def toJson: String = new GsonBuilder().setPrettyPrinting().create().toJson(instance)

  def write(): Unit = {
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