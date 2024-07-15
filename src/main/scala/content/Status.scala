package content

import javafx.application.Platform
import javafx.scene.control.TextArea

object Status {
  private var statusArea: TextArea = _
  private val maxHeight = 100d
  private val maxLines = 5

  def initialize(textArea: TextArea): Unit = {
    statusArea = textArea
    statusArea.setEditable(false)
    statusArea.setWrapText(true)
    statusArea.setMaxHeight(maxHeight)
  }

  def appendText(text: String): Unit = {
    if (statusArea != null) {
      Platform.runLater(() => {
        if (statusArea.getText.isEmpty) {
          statusArea.setText(text)
        } else {
          statusArea.appendText("\n" + text)
        }
      })
    }
  }

  def notifyTimeout(): Unit = {
    appendText("Connection timed out. Please check your network and try again.")
  }

  def clear(): Unit = {
    if (statusArea != null) {
      Platform.runLater(() => statusArea.clear())
    }
  }

  def getText: String = {
    if (statusArea != null) statusArea.getText else ""
  }
}