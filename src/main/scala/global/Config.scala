package global

import com.google.gson.Gson
import scala.util.{Try, Using}

case class SshConfig(autoConnect: Boolean, configFile: String)
case class OutConfig(cache: Boolean, log: Boolean, dbg: Boolean)
case class DirConfig(downloadsDir: String, cacheDir: String, dbgDir: String)

class Config(
              val project: String,
              val ssh: SshConfig,
              val out: OutConfig,
              val dir: DirConfig
            )

object Config {
  private var instance: Option[Config] = None
  // デフォルト値の定義
  private val defaultConfig = new Config(
    project = "Asap",
    ssh = SshConfig(autoConnect = true, configFile = "ssh.json"),
    out = OutConfig(
      cache = true,
      log = true,
      dbg = false
    ),
    dir = DirConfig(
      downloadsDir = "downloads",
      cacheDir = "cache",
      dbgDir = "debug"
    )
  )

  def apply(path: String): Config = {
    instance.getOrElse {
      val config = loadConfig(path).getOrElse {
        println(s"Warning: Failed to load config from $path. Using default configuration.")
        defaultConfig
      }
      instance = Some(config)
      config
    }
  }

  private def loadConfig(path: String): Try[Config] = {
    val gson = new Gson()
    Using(scala.io.Source.fromFile(path)) { source =>
      gson.fromJson(source.mkString, classOf[Config])
    }
  }

  def project: String = instance.map(_.project).getOrElse(defaultConfig.project)
  def ssh: SshConfig = instance.map(_.ssh).getOrElse(defaultConfig.ssh)
  def out: OutConfig = instance.map(_.out).getOrElse(defaultConfig.out)
  def dir: DirConfig = instance.map(_.dir).getOrElse(defaultConfig.dir)
}