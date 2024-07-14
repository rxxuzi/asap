import javafx.geometry.Insets
import javafx.scene.control.{Button, Label, TextArea, TextField}
import javafx.scene.layout.{HBox, VBox}
import ssh.SSHManager

class HomeTab(sshManager: SSHManager, updateTitle: () => Unit) {
  private val statusArea = new TextArea()
  statusArea.setEditable(false)
  statusArea.setPrefRowCount(10)

  def getContent: VBox = {
    val sshConfigPath = new TextField()
    sshConfigPath.setPromptText("Path to SSH config JSON")

    val connectButton = new Button("Connect")

    connectButton.setOnAction(_ => {
      sshManager.connect(sshConfigPath.getText) match {
        case scala.util.Success(info) => {
          statusArea.appendText(s"Connected: $info\n")
          updateTitle()
        }
        case scala.util.Failure(ex) => statusArea.appendText(s"Connection failed: ${ex.getMessage}\n")
      }
    })

    val content = new VBox(10)
    content.setPadding(new Insets(10))
    content.getChildren.addAll(
      new HBox(10, new Label("SSH Config:"), sshConfigPath, connectButton),
      statusArea
    )

    content
  }
}