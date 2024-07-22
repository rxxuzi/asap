import content.{AudioPlayer, VideoPlayer}
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.{ScrollPane, TextArea}
import javafx.scene.image.{Image, ImageView}
import javafx.scene.layout.{StackPane, VBox}
import javafx.scene.text.Text
import security.Hash
import style.Style

import java.io.File
import java.nio.file.{Files, Path}
import scala.util.Try

final class FileContentViewer(maxWidth: Double, maxHeight: Double) {
  private val imageExtensions = Set(".jpg", ".jpeg", ".png", ".gif", ".bmp")
  private val audioExtensions = Set(".mp3", ".wav", ".aac", ".ogg")
  private val videoExtensions = Set(".mp4", ".avi", ".mov", ".wmv")
  private val textExtensions = Set(".txt", ".py", ".html", ".css", ".js", ".json", ".xml", ".md", ".scala", ".java", ".c", ".cpp", ".h", ".sh", ".bat", ".csv")

  private val binaryImage = new Image(getClass.getResourceAsStream("png/object.png"))

  def viewContent(file: File): Node = {
    val extension = getFileExtension(file.getName).toLowerCase
    
    val content = if (imageExtensions.contains(extension)) {
      createImageView(file)
    } else if (audioExtensions.contains(extension)) {
      createAudioView(file)
    } else if (videoExtensions.contains(extension)) {
      createVideoView(file)
    } else if (isTextFile(file.toPath)) {
      createTextView(file)
    } else {
      createBinaryFileView(file)
    }

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

  private def createImageView(file: File): Node = {
    val image = new Image(file.toURI.toString)
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

  private def createAudioView(file: File): Node = {
    new AudioPlayer(file)
  }

  private def createVideoView(file: File): Node = {
    new VideoPlayer(file)
  }

  private def createTextView(file: File): Node = {
    val content = new String(Files.readAllBytes(file.toPath))
    val textArea = new TextArea(content)
    textArea.setEditable(false)
    textArea.setWrapText(true)
    textArea.setStyle("-fx-background-color: -fx-background; -fx-text-fill: -fx-text-base-color;")
    textArea
  }

  private def createBinaryFileView(file: File): Node = {
    val imageView = new ImageView(binaryImage)
    imageView.setFitWidth(256)
    imageView.setFitHeight(256)
    imageView.setPreserveRatio(true)
    
    val name = "Binary File: " + file.getName
    val size = "File Size : " + Files.size(file.toPath)
    val hash = "SHA-256   : " + Hash.sha256(file)
    

    val text = new Text((name + "\n" + size + "\n" + hash).stripMargin)

    text.setFont(Style.opFont)

    val vbox = new VBox(10, imageView, text)
    vbox.setAlignment(Pos.CENTER)
    vbox
  }
}