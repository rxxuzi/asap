import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.scene.control.*
import javafx.scene.layout.{HBox, VBox}
import javafx.stage.FileChooser
import ssh.SSHManager

import java.io.File

class SendTab(sshManager: SSHManager, updateTitle: () => Unit) {
  private val statusArea = new TextArea()
  statusArea.setEditable(false)
  statusArea.setPrefRowCount(10)

  private val remoteDirComboBox = new ComboBox[String](FXCollections.observableArrayList())
  remoteDirComboBox.setPromptText("Select Remote Directory")
  remoteDirComboBox.setEditable(true)

  def getContent: VBox = {
    val localFilePathField = new TextField()
    localFilePathField.setPromptText("Local file path")
    val chooseFileButton = new Button("Choose File")

    val sendButton = new Button("Send File")
    val listFilesButton = new Button("List Remote Files")

    chooseFileButton.setOnAction(_ => {
      val fileChooser = new FileChooser()
      fileChooser.setTitle("Select File")
      val selectedFile = fileChooser.showOpenDialog(null)
      if (selectedFile != null) {
        localFilePathField.setText(selectedFile.getAbsolutePath)
      }
    })

    sendButton.setOnAction(_ => {
      if (sshManager.isConnected) {
        sshManager.withSSH { ssh =>
          val localFile = new File(localFilePathField.getText)
          val remoteDir = remoteDirComboBox.getValue
          val remotePath = if (remoteDir == "./") localFile.getName else s"$remoteDir/${localFile.getName}"
          ssh.send(localFile, remotePath)
          statusArea.appendText(s"File sent: ${localFile.getAbsolutePath} -> $remotePath\n")
          updateRemoteDirList()
        }.recover {
          case ex => statusArea.appendText(s"Send failed: ${ex.getMessage}\n")
        }
      } else {
        statusArea.appendText("Not connected. Please connect to SSH first.\n")
      }
    })

    listFilesButton.setOnAction(_ => updateRemoteDirList())

    val content = new VBox(10)
    content.setPadding(new Insets(10))
    content.getChildren.addAll(
      new HBox(10, new Label("Local File:"), localFilePathField, chooseFileButton),
      new HBox(10, new Label("Remote Dir:"), remoteDirComboBox),
      new HBox(10, sendButton, listFilesButton),
      statusArea
    )

    content
  }

  private def updateRemoteDirList(): Unit = {
    if (sshManager.isConnected) {
      sshManager.withSSH { ssh =>
        val homeDir = ssh.exec("echo $HOME").trim
        val directories = "./" :: ssh.listFiles(homeDir)
          .filter(_.isDir)
          .map(_.name)
          .filter(dir => !dir.startsWith(".") && !List("..").contains(dir))
        val items = FXCollections.observableArrayList(directories: _*)
        remoteDirComboBox.setItems(items)
        remoteDirComboBox.setValue("./")
        statusArea.appendText("Remote directory list updated.\n")
      }.recover {
        case ex => statusArea.appendText(s"Failed to update remote directory list: ${ex.getMessage}\n")
      }
    } else {
      statusArea.appendText("Not connected. Please connect to SSH first.\n")
    }
  }
}