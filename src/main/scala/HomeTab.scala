import javafx.geometry.Insets
import javafx.scene.control.{Button, Label, TextArea, TextField}
import javafx.scene.layout.{HBox, VBox}
import ssh.SSHManager
import content.Status

class HomeTab(sshManager: SSHManager) {
  def getContent: VBox = {
    val sshConfigPath = new TextField()
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

    val content = new VBox(10)
    content.setPadding(new Insets(10))
    content.getChildren.addAll(
      new HBox(10, new Label("SSH Config:"), sshConfigPath, connectButton)
    )

    content
  }
}