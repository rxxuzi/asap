package global

import java.io.File

object IO {
  def mkdir(path: String): Boolean = {
    val dir = new File(path)
    if(!dir.exists()) dir.mkdir()
    else false
  }

  def listFiles(dir: String): Seq[File] = {
    val d = new File(dir)
    if (d.exists && d.isDirectory) {
      d.listFiles.flatMap {
        case f if f.isDirectory => listFiles(f.getPath)
        case f => Seq(f)
      }
    } else {
      Seq.empty
    }
  }
}
