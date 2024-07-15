import javafx.geometry.Insets
import javafx.scene.control.{Button, Label, TextArea, TextField, ToggleButton}
import javafx.scene.layout.{HBox, VBox}
import ssh.SSHManager
import content.Status
import style.{CSS, Style}
import javafx.scene.Scene
import javafx.scene.web.{WebEngine, WebView}

class HomeTab(sshManager: SSHManager, scene: Scene) {
  def getContent: VBox = {
    val sshConfigPath = new TextField()
    val configLabel = new Label("SSH Config:")
    sshConfigPath.setPromptText("Path to SSH config JSON")

    val connectButton = new Button("Connect")

    connectButton.setOnAction(_ => {
      if (!sshManager.isConnected) {
        sshManager.connect(sshConfigPath.getText) match {
          case scala.util.Success(info) => Status.appendText(s"Connected: $info")
          case scala.util.Failure(ex) => Status.appendText(s"Connection failed: ${ex.getMessage}")
        }
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
}