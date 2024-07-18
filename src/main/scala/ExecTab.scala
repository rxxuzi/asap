import global.Log
import javafx.application.Platform
import javafx.geometry.{Insets, Pos}
import javafx.scene.control.{Button, Label, TextArea, TextField}
import javafx.scene.input.KeyCode
import javafx.scene.layout.{HBox, VBox}
import ssh.SSHManager

class ExecTab(sshManager: SSHManager) {
  private val commandField = new TextField()
  commandField.setPromptText("Enter command")

  private val execButton = new Button("Execute")

  private val outputArea = new TextArea()
  outputArea.setEditable(false)
  outputArea.setPrefRowCount(20)

  private val pathLabel = new Label()

  def getContent: VBox = {
    execButton.setOnAction(_ => executeCommand())

    // Improve key event handler for Enter key
    commandField.setOnKeyPressed(event => {
      if (event.getCode == KeyCode.ENTER) {
        event.consume() // Prevent the default action
        Platform.runLater(() => executeCommand()) // Execute on the JavaFX Application Thread
      }
    })

    val inputBox = new HBox(10)
    inputBox.getChildren.addAll(commandField, execButton)
    inputBox.setAlignment(Pos.CENTER_LEFT)
    HBox.setHgrow(commandField, javafx.scene.layout.Priority.ALWAYS)

    val content = new VBox(10)
    content.setPadding(new Insets(10))
    content.getChildren.addAll(pathLabel, inputBox, outputArea)
    VBox.setVgrow(outputArea, javafx.scene.layout.Priority.ALWAYS)

    updatePathLabel()
    sshManager.addConnectionListener(() => updatePathLabel())

    content
  }

  private def updatePathLabel(): Unit = {
    pathLabel.setText(s"Current path: ${sshManager.getCurrentPath}")
  }

  private def executeCommand(): Unit = {
    val command = commandField.getText.trim
    if (command.nonEmpty) {
      if (command == "clear") {
        outputArea.clear()
        Log.info("Output area cleared")
      } else if (sshManager.isConnected) {
        sshManager.withSSH { ssh =>
          outputArea.appendText(s"\n> $command\n")

          val (output, newPath) = if (command.startsWith("cd ")) {
            val cdCommand = s"cd ${sshManager.getCurrentPath} && $command && pwd"
            val result = ssh.exec(cdCommand)
            val lines = result.split("\n")
            val newPath = lines.last.trim
            (lines.init.mkString("\n"), newPath)
          } else {
            val fullCommand = s"cd ${sshManager.getCurrentPath} && $command"
            (ssh.exec(fullCommand), sshManager.getCurrentPath)
          }



          outputArea.appendText(s"$output\n")
          if (command.equals("clear")) {

          }
          sshManager.updatePath(newPath)
          updatePathLabel()
        }.recover {
          case ex =>
            outputArea.appendText(s"Execution failed: ${ex.getMessage}\n")
            Log.err(s"Command execution failed: ${ex.getMessage}")
        }
      } else {
        outputArea.appendText("Not connected. Please connect to SSH first.\n")
        Log.err("Cannot execute command: Not connected to SSH")
      }
    } else {
      outputArea.appendText("Please enter a command.\n")
      Log.err("Cannot execute empty command")
    }
    commandField.clear()
    commandField.requestFocus()
  }
}