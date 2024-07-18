import global.Log
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.{Button, Label, TextField, ToggleButton}
import javafx.scene.layout.{HBox, VBox}
import ssh.SSHManager
import style.{CSS, Style}

class HomeTab(sshManager: SSHManager, scene: Scene) {
  private val connectButton = new Button("Connect")
  private val sshConfigPath = new TextField()

  def getContent: VBox = {
    val configLabel = new Label("SSH Config:")
    sshConfigPath.setPromptText("Path to SSH config JSON")

    updateConnectButtonState()

    connectButton.setOnAction(_ => {
      if (!sshManager.isConnected) {
        connect()
      } else {
        disconnect()
      }
    })

    val styleToggle = new ToggleButton("Toggle Dark Mode")
    styleToggle.setSelected(Style.getCurrentStyle == CSS.DARK)
    styleToggle.setOnAction(_ => {
      Style.toggleStyle(scene)
      styleToggle.setSelected(Style.getCurrentStyle == CSS.DARK)
    })

    val content = new VBox(10)
    content.setPadding(new Insets(10))
    content.getChildren.addAll(
      new HBox(10, configLabel, sshConfigPath, connectButton),
      styleToggle
    )
    content
  }

  private def connect(): Unit = {
    sshManager.connect(sshConfigPath.getText) match {
      case scala.util.Success(info) =>
        Log.info(s"Connected: $info")
        updateConnectButtonState()
      case scala.util.Failure(ex) =>
        Log.err(s"Connection failed: ${ex.getMessage}")
    }
  }

  private def disconnect(): Unit = {
    sshManager.disconnect()
    Log.info("Disconnected from SSH")
    updateConnectButtonState()
  }

  private def updateConnectButtonState(): Unit = {
    if (sshManager.isConnected) {
      connectButton.setText("Disconnect")
      sshConfigPath.setDisable(true)
    } else {
      connectButton.setText("Connect")
      sshConfigPath.setDisable(false)
    }
  }
}