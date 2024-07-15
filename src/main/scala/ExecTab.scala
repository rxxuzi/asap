import javafx.scene.layout.{VBox, HBox}
import javafx.scene.control.{TextField, Button, TextArea}
import javafx.geometry.Insets
import ssh.SSHManager
import content.Status

class ExecTab(sshManager: SSHManager) {
  private val commandField = new TextField()
  commandField.setPromptText("Enter command")

  private val execButton = new Button("Exec")

  private val outputArea = new TextArea()
  outputArea.setEditable(false)
  outputArea.setPrefRowCount(20)

  def getContent: VBox = {
    execButton.setOnAction(_ => executeCommand())

    val inputBox = new HBox(10)
    inputBox.getChildren.addAll(commandField, execButton)
    HBox.setHgrow(commandField, javafx.scene.layout.Priority.ALWAYS)

    val content = new VBox(10)
    content.setPadding(new Insets(10))
    content.getChildren.addAll(inputBox, outputArea)
    VBox.setVgrow(outputArea, javafx.scene.layout.Priority.ALWAYS)

    content
  }

  private def executeCommand(): Unit = {
    val command = commandField.getText
    if (command.nonEmpty) {
      if (sshManager.isConnected) {
        sshManager.withSSH { ssh =>
          outputArea.appendText(s"> $command")
          val output = ssh.exec(command)
          outputArea.appendText(s"$output")

          if (command.trim.startsWith("cd ")) {
            val newPath = ssh.exec("pwd").trim
            sshManager.updatePath(newPath)
          }
        }.recover {
          case ex => outputArea.appendText(s"Execution failed: ${ex.getMessage}")
        }
      } else {
        outputArea.appendText("Not connected. Please connect to SSH first.")
      }
    } else {
      outputArea.appendText("Please enter a command.")
    }
    commandField.clear()
  }
}