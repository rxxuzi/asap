package global

import java.io.{BufferedWriter, FileWriter}
import java.text.SimpleDateFormat
import java.util.Date
import java.nio.file.{Files, Paths}
import scala.concurrent.{Future, ExecutionContext}
import ExecutionContext.Implicits.global

// ログタイプを定義するEnum
enum Log {
  case Error, Debug, Info
}

object Log {
  private var logFilePath: String = _

  def init(dirPath: String): Unit = {
    val dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss")
    val dateStr = dateFormat.format(new Date())
    val dir = Paths.get(dirPath)
    if (!Files.exists(dir)) {
      Files.createDirectories(dir)
    }
    logFilePath = s"$dirPath/asap-$dateStr.log"
  }

  def append(logType: Log, message: String): Unit = {
    if (logFilePath == null) {
      init(Config.defaultLogDir)
    }
    Future {
      val writer = new BufferedWriter(new FileWriter(logFilePath, true))
      try {
        val timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
        writer.write(s"[$timeStamp] [$logType] $message")
        writer.newLine()
      } finally {
        writer.close()
      }
    }
  }

  def append(message: String): Unit = append(Log.Info, message)
}
