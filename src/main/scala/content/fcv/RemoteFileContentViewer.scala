package content.fcv

import content.fcv.FileContentViewer
import content.{AudioPlayer, VideoPlayer}
import global.Config
import javafx.scene.Node
import javafx.scene.image.Image
import ssh.{RemoteFile, SSH}

import java.nio.file.{Files, Path}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

final class RemoteFileContentViewer(override val maxWidth: Double, override val maxHeight: Double, ssh: SSH) extends FileContentViewer {

  override def viewContent(file: Any): Future[Node] = {
    val remoteFile = file.asInstanceOf[RemoteFile]
    val extension = getFileExtension(remoteFile.name).toLowerCase

    for {
      isText <- isTextFile(remoteFile)
      content <- if (imageExtensions.contains(extension)) {
        viewImageContent(remoteFile)
      } else if (audioExtensions.contains(extension) || videoExtensions.contains(extension)) {
        viewMediaContent(remoteFile)
      } else if (isText) {
        viewTextContent(remoteFile)
      } else {
        viewBinaryFileContent(remoteFile)
      }
    } yield wrapInScrollPane(content)
  }

  private def isTextFile(file: RemoteFile): Future[Boolean] = {
    val fileName = file.name.toLowerCase
    if (textExtensions.exists(ext => fileName.endsWith(ext))) {
      Future.successful(true)
    } else if (file.size > MAX_TEXT_FILE_SIZE) {
      Future.successful(false)
    } else {
      Future {
        val content = ssh.exec(s"head -c 1000 '${file.fullPath}' | LC_ALL=C grep -q '[^[:print:][:space:]]' && echo 'binary' || echo 'text'")
        content.trim == "text"
      }.recover {
        case _ => false
      }
    }
  }

  private def viewMediaContent(file: RemoteFile): Future[Node] = Future {
    val tempDir : Path = Config.tmpDir
    val tempFile = tempDir.resolve(file.name)

    ssh.get(file.fullPath, tempFile.toString)
    val mediaPlayer = if (audioExtensions.contains(getFileExtension(file.name).toLowerCase)) {
      new AudioPlayer(tempFile.toFile)
    } else {
      new VideoPlayer(tempFile.toFile)
    }

    addShutdownHook(tempFile, tempDir)

    mediaPlayer
  }

  private def viewImageContent(file: RemoteFile): Future[Node] = Future {
    val tempDir : Path = Config.tmpDir
    val tempFile = tempDir.resolve(file.name)

    try {
      ssh.get(file.fullPath, tempFile.toString)
      val image = new Image(tempFile.toUri.toString)
      createImageView(image)
    } finally {
      addShutdownHook(tempFile, tempDir)
    }
  }

  private def viewTextContent(file: RemoteFile): Future[Node] = Future {
    val content = if (file.size > MAX_TEXT_FILE_SIZE) {
      ssh.exec(s"head -c $MAX_PREVIEW_SIZE '${file.fullPath}'")
    } else {
      ssh.exec(s"cat '${file.fullPath}'")
    }

    if (file.size > MAX_TEXT_FILE_SIZE) {
      createLargeFileView(file.name, file.size, content)
    } else {
      createTextArea(content)
    }
  }

  private def viewBinaryFileContent(file: RemoteFile): Future[Node] = Future {
    val hash = ssh.exec(s"sha256sum '${file.fullPath}' | cut -d' ' -f1")
    createBinaryFileView(file.name, file.size, hash)
  }

  private def addShutdownHook(tempFile: Path, tempDir: Path): Unit = {
    Runtime.getRuntime.addShutdownHook(new Thread(new Runnable {
      override def run(): Unit = {
        try {
          Files.deleteIfExists(tempFile)
          Files.deleteIfExists(tempDir)
        } catch {
          case e: Exception => e.printStackTrace()
        }
      }
    }))
  }
}