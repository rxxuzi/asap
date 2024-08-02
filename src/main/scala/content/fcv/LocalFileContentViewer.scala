package content.fcv

import content.fcv.FileContentViewer
import content.{AudioPlayer, VideoPlayer}
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.layout.VBox
import security.Hash

import java.io.{File, RandomAccessFile}
import java.nio.file.{Files, Path}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

class LocalFileContentViewer(override val maxWidth: Double, override val maxHeight: Double) extends FileContentViewer {

  override def viewContent(file: Any): Future[Node] = Future {
    val localFile = file.asInstanceOf[File]
    val extension = getFileExtension(localFile.getName).toLowerCase

    val content = if (imageExtensions.contains(extension)) {
      createImageView(new javafx.scene.image.Image(localFile.toURI.toString))
    } else if (audioExtensions.contains(extension)) {
      new AudioPlayer(localFile)
    } else if (videoExtensions.contains(extension)) {
      new VideoPlayer(localFile)
    } else if (isTextFile(localFile.toPath)) {
      createTextView(localFile)
    } else {
      createBinaryFileView(localFile.getName, localFile.length(), Hash.sha256(localFile))
    }

    wrapInScrollPane(content)
  }

  private def isTextFile(file: Path): Boolean = {
    val fileName = file.getFileName.toString.toLowerCase
    if (textExtensions.exists(ext => fileName.endsWith(ext))) {
      return true
    }

    Try {
      val bytes = Files.readAllBytes(file)
      val n = Math.min(bytes.length, 1000) // Check only first 1000 bytes
      val numOfNonPrintable = bytes.take(n).count(b => b < 32 && b != 9 && b != 10 && b != 13)
      numOfNonPrintable.toDouble / n < 0.05 // If less than 5% non-printable characters, consider it text
    }.getOrElse(false)
  }

  private def createTextView(file: File): Node = {
    if (file.length() > MAX_TEXT_FILE_SIZE) {
      val content = createLargeFileView(file.getName, file.length(), readFilePreview(file, MAX_PREVIEW_SIZE))
      centerContent(content)
    } else {
      val textArea = createTextArea(new String(Files.readAllBytes(file.toPath)))
      centerContent(textArea)
    }
  }

  private def centerContent(content: Node): Node = {
    val vBox = new VBox(content)
    vBox.setAlignment(Pos.CENTER)
    vBox.setFillWidth(true)
    vBox
  }

  private def readFilePreview(file: File, maxBytes: Int): String = {
    val buffer = new Array[Byte](maxBytes)
    val raf = new RandomAccessFile(file, "r")
    try {
      raf.readFully(buffer)
      new String(buffer)
    } finally {
      raf.close()
    }
  }
}