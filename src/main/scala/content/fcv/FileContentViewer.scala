package content.fcv

import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.{ScrollPane, TextArea}
import javafx.scene.image.{Image, ImageView}
import javafx.scene.layout.{StackPane, VBox}
import javafx.scene.text.Text
import style.Style

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait FileContentViewer {
  protected val imageExtensions: Set[String] = Set(".jpg", ".jpeg", ".png", ".gif", ".bmp")
  protected val audioExtensions: Set[String] = Set(".mp3", ".wav", ".aac", ".ogg")
  protected val videoExtensions: Set[String] = Set(".mp4", ".avi", ".mov", ".wmv")
  protected val textExtensions: Set[String] = Set(".txt", ".py", ".html", ".css", ".js", ".json", ".xml", ".md", ".scala", ".java", ".c", ".cpp", ".h", ".sh", ".bat", ".csv")

  protected val MAX_TEXT_FILE_SIZE: Long = 10 * 1024 * 1024 // 10 MB
  protected val MAX_PREVIEW_SIZE: Int = 1024 * 10 // 10 KB

  protected val binaryImage: Image = new Image(getClass.getResourceAsStream("/png/object.png"))

  protected val maxWidth: Double
  protected val maxHeight: Double

  def viewContent(file: Any): Future[Node]

  protected def getFileExtension(fileName: String): String = {
    val lastDotIndex = fileName.lastIndexOf('.')
    if (lastDotIndex > 0 && lastDotIndex < fileName.length - 1) {
      fileName.substring(lastDotIndex)
    } else {
      ""
    }
  }

  protected def formatFileSize(size: Long): String = {
    if (size < 1024) return s"$size B"
    val kb = size / 1024.0
    if (kb < 1024) return f"$kb%.1f KB"
    val mb = kb / 1024.0
    if (mb < 1024) return f"$mb%.1f MB"
    val gb = mb / 1024.0
    f"$gb%.1f GB"
  }

  protected def wrapInScrollPane(content: Node): ScrollPane = {
    val stackPane = new StackPane(content)
    stackPane.setAlignment(Pos.CENTER)

    val scrollPane = new ScrollPane(stackPane)
    scrollPane.setFitToWidth(true)
    scrollPane.setFitToHeight(true)
    scrollPane
  }

  protected def createImageView(image: Image): Node = {
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

  protected def createTextArea(content: String): TextArea = {
    val textArea = new TextArea(content)
    textArea.setEditable(false)
    textArea.setWrapText(true)
    textArea.setStyle("-fx-background-color: -fx-background; -fx-text-fill: -fx-text-base-color;")
    textArea
  }

  protected def createLargeFileView(fileName: String, fileSize: Long, content: String): Node = {
    val textArea = createTextArea(content)
    val infoText = new Text(s"Large file: $fileName\nSize: ${formatFileSize(fileSize)}\nShowing first ${MAX_PREVIEW_SIZE / 1024} KB")
    infoText.setFont(Style.opFont)
    new VBox(10, infoText, textArea)
  }

  protected def createBinaryFileView(fileName: String, fileSize: Long, hash: String): Node = {
    val imageView = new ImageView(binaryImage)
    imageView.setFitWidth(256)
    imageView.setFitHeight(256)
    imageView.setPreserveRatio(true)

    val name = "Binary File: " + fileName
    val size = "File Size : " + formatFileSize(fileSize)
    val text = new Text((name + "\n" + size + "\n" + "SHA-256   : " + hash).stripMargin)
    text.setFont(Style.opFont)

    val vbox = new VBox(10, imageView, text)
    vbox.setAlignment(Pos.CENTER)
    vbox
  }
}