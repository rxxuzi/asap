package content

import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.{ScrollPane, TextArea}
import javafx.scene.image.{Image, ImageView}
import javafx.scene.layout.{StackPane, VBox}
import javafx.scene.text.Text
import ssh.{RemoteFile, SSH}
import style.Style

import java.nio.file.Files
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

final class RemoteFileContentViewer(maxWidth: Double, maxHeight: Double, ssh: SSH) {
  private val imageExtensions = Set(".jpg", ".jpeg", ".png", ".gif", ".bmp")
  private val audioExtensions = Set(".mp3", ".wav", ".aac", ".ogg")
  private val videoExtensions = Set(".mp4", ".avi", ".mov", ".wmv")
  private val textExtensions = Set(".txt", ".py", ".html", ".css", ".js", ".json", ".xml", ".md", ".scala", ".java", ".c", ".cpp", ".h", ".sh", ".bat", ".csv")

  private val binaryImage = new Image(getClass.getResourceAsStream("/png/object.png"))
  val tmpDir = "asap-ssh-viewer"

  def viewContent(file: RemoteFile): Future[Node] = {
    val extension = getFileExtension(file.name).toLowerCase

    for {
      isText <- isTextFile(file)
      content <- if (imageExtensions.contains(extension)) {
        viewImageContent(file)
      } else if (audioExtensions.contains(extension) || videoExtensions.contains(extension)) {
        viewMediaContent(file)
      } else if (isText) {
        viewTextContent(file)
      } else {
        viewBinaryFileContent(file)
      }
    } yield wrapInScrollPane(content)
  }

  private def wrapInScrollPane(content: Node): ScrollPane = {
    val stackPane = new StackPane(content)
    stackPane.setAlignment(Pos.CENTER)

    val scrollPane = new ScrollPane(stackPane)
    scrollPane.setFitToWidth(true)
    scrollPane.setFitToHeight(true)
    scrollPane
  }

  private def getFileExtension(fileName: String): String = {
    val lastDotIndex = fileName.lastIndexOf('.')
    if (lastDotIndex > 0 && lastDotIndex < fileName.length - 1) {
      fileName.substring(lastDotIndex)
    } else {
      ""
    }
  }

  private def isTextFile(file: RemoteFile): Future[Boolean] = {
    val fileName = file.name.toLowerCase
    if (textExtensions.exists(ext => fileName.endsWith(ext))) {
      Future.successful(true)
    } else if (file.size > 1024 * 1024) { // Don't check files larger than 1MB
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
    val tempDir = Files.createTempDirectory(tmpDir)
    val tempFile = tempDir.resolve(file.name)

    ssh.get(file.fullPath, tempFile.toString)
    val mediaPlayer = if (audioExtensions.contains(getFileExtension(file.name).toLowerCase)) {
      new AudioPlayer(tempFile.toFile)
    } else {
      new VideoPlayer(tempFile.toFile)
    }

    // ファイルを削除するためのシャットダウンフックを追加
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

    mediaPlayer
  }

  private def viewImageContent(file: RemoteFile): Future[Node] = Future {
    val tempDir = Files.createTempDirectory(tmpDir)
    val tempFile = tempDir.resolve(file.name)

    try {
      ssh.get(file.fullPath, tempFile.toString)
      val image = new Image(tempFile.toUri.toString)
      createImageView(image)
    } finally {
      // ファイルを削除するためのシャットダウンフックを追加
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

  private def createImageView(image: Image): Node = {
    val imageView = new ImageView(image)
    imageView.setPreserveRatio(true)

    if (image.getWidth > maxWidth || image.getHeight > maxHeight) {
      val widthRatio = maxWidth / image.getWidth
      val heightRatio = maxHeight / image.getHeight
      val scale = Math.min(widthRatio, heightRatio)

      imageView.setFitWidth(image.getWidth * scale)
      imageView.setFitHeight(image.getHeight * scale)
    }

    imageView
  }

  private def viewTextContent(file: RemoteFile): Future[Node] = Future {
    val content = ssh.exec(s"cat '${file.fullPath}'")
    val textArea = new TextArea(content)
    textArea.setEditable(false)
    textArea.setWrapText(true)
    textArea.setStyle("-fx-background-color: -fx-background; -fx-text-fill: -fx-text-base-color;")
    textArea
  }

  private def viewBinaryFileContent(file: RemoteFile): Future[Node] = Future {
    val imageView = new ImageView(binaryImage)
    imageView.setFitWidth(256)
    imageView.setFitHeight(256)
    imageView.setPreserveRatio(true)

    val name = "Binary File: " + file.name
    val size = "File Size : " + (if (file.size >= 0) file.size.toString else "Unknown")
    val hash = "SHA-256   : " + ssh.exec(s"sha256sum '${file.fullPath}' | cut -d' ' -f1")

    val text = new Text((name + "\n" + size + "\n" + hash).stripMargin)
    text.setFont(Style.opFont)

    val vbox = new VBox(10, imageView, text)
    vbox.setAlignment(Pos.CENTER)
    vbox
  }
}