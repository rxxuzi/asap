package global

import content.Status

import java.io.{BufferedWriter, FileWriter}
import java.text.SimpleDateFormat
import java.util.Date
import java.nio.file.{Files, Paths}
import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global

// ログタイプを定義するEnum
enum Log {
  case Error, Debug, Info, Warn
}

object Log {
  private var logFilePath: String = _
  private var mk : Boolean = false

  def init(dirPath: String): Unit = {
    val dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss")
    val dateStr = dateFormat.format(new Date())
    val dir = Paths.get(dirPath)
    if (!Files.exists(dir)) {
      Files.createDirectories(dir)
    }
    logFilePath = s"$dirPath/asap-$dateStr.log"
    mk = true
  }

  private def append(logType: Log, message: String, at: Boolean): Unit = {
    if (at) Status.appendText(message)
    if (mk) {
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
  }
  
  private def append(log: Log, message: String) : Unit = append(log,message,true)

  def err(msg: String) : Unit = {append(Log.Error, msg); System.err.println(msg)}
  def warn(msg: String) : Unit = append(Log.Warn, msg)
  def info(msg: String) : Unit = append(Log.Info, msg)
  def dbg(msg: String) : Unit = {append(Log.Debug, msg, false); println(msg)}
  def apt(msg: String) : Unit = Status.appendText(msg)
}
