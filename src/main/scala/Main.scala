import global.{Config, Log}
import io.IO
import javafx.application.Application

object Main {
  private def init(): Unit = {
    Config.initialize()
    IO.mkdir(Config.dir.downloadsDir)
    if (Config.out.cache) IO.mkdir(Config.dir.cacheDir)
    if (Config.out.dbg) IO.mkdir(Config.dir.dbgDir)
    if (Config.out.log) {
      IO.mkdir(Config.dir.logDir)
      Log.init(Config.dir.logDir)
    }
  }

  def main(args: Array[String]): Unit = {
    init()
    Application.launch(classOf[Core], args: _*)
  }
}