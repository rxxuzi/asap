import content.RemoteFile
import content.Tree.addPathToTree
import global.IO
import javafx.scene.layout.{HBox, Priority, VBox}
import javafx.scene.control.{Button, Label, TextArea, TextField, TreeItem, TreeView}
import javafx.geometry.Insets
import ssh.SSHManager

import java.io.File
import java.nio.file.Paths
import scala.jdk.CollectionConverters.*
import javafx.application.Platform

import scala.util.{Failure, Success, Try}
import javafx.scene.control.cell.TextFieldTreeCell
import javafx.util.StringConverter

class GetTab(sshManager: SSHManager, updateTitle: () => Unit) {
  private val statusArea = new TextArea()
  statusArea.setEditable(false)
  statusArea.setPrefRowCount(5)

  private val asTextField = new TextField()
  asTextField.setPromptText("Save as...")

  private val fileTreeView = new TreeView[RemoteFile]()
  fileTreeView.setShowRoot(false)
  fileTreeView.setCellFactory(_ => new TextFieldTreeCell[RemoteFile](new StringConverter[RemoteFile]() {
    override def toString(rf: RemoteFile): String = rf.name
    override def fromString(string: String): RemoteFile = null // Not used for this example
  }))

  def getContent: VBox = {
    val getButton = new Button("Get")
    val refreshButton = new Button("Refresh")

    getButton.setOnAction(_ => downloadSelectedFile())
    refreshButton.setOnAction(_ => updateFileTree())

    val content = new VBox(10)
    content.setPadding(new Insets(10))
    content.getChildren.addAll(
      new HBox(10, refreshButton),
      fileTreeView,
      new HBox(10, new Label("Save as:"), asTextField, getButton),
      statusArea
    )

    VBox.setVgrow(fileTreeView, Priority.ALWAYS)

    content
  }

  private def downloadSelectedFile(): Unit = {
    if (sshManager.isConnected) {
      val selectedItem = fileTreeView.getSelectionModel.getSelectedItem
      if (selectedItem != null && !selectedItem.getValue.isDirectory) {
        val remoteFile = selectedItem.getValue.fullPath
        val saveAsName = if (asTextField.getText.nonEmpty) asTextField.getText else new File(remoteFile).getName
        val localPath = Paths.get("downloads", saveAsName).toString

        IO.mkdir("downloads")

        sshManager.withSSH { ssh =>
          Try {
            ssh.get(remoteFile, localPath)
            Platform.runLater(() => {
              statusArea.appendText(s"File downloaded: $remoteFile -> $localPath\n")
            })
          }.recover {
            case ex => Platform.runLater(() => {
              statusArea.appendText(s"Download failed: ${ex.getMessage}\n")
            })
          }
        }.recover {
          case ex => Platform.runLater(() => {
            statusArea.appendText(s"SSH operation failed: ${ex.getMessage}\n")
          })
        }
      } else {
        statusArea.appendText("Please select a file to download.\n")
      }
    } else {
      statusArea.appendText("Not connected. Please connect to SSH first.\n")
    }
  }

  private def updateFileTree(): Unit = {
    if (sshManager.isConnected) {
      sshManager.withSSH { ssh =>
        Try {
          val homeDir = ssh.exec("echo $HOME").trim
          val fileList = ssh.exec(s"find $homeDir -type d -o -type f").split("\n").filter(_.nonEmpty)
          val rootItem = new TreeItem[RemoteFile](RemoteFile("Root", homeDir, isDirectory = true))

          fileList.foreach { path =>
            val relativePath = path.replace(homeDir, "").stripPrefix("/")
            addPathToTree(rootItem, relativePath, path, homeDir)
          }

          Platform.runLater(() => {
            fileTreeView.setRoot(rootItem)
            statusArea.appendText("Remote file tree updated.\n")
          })
        }.recover {
          case ex => Platform.runLater(() => {
            statusArea.appendText(s"Failed to list remote files: ${ex.getMessage}\n")
          })
        }
      }.recover {
        case ex => Platform.runLater(() => {
          statusArea.appendText(s"SSH operation failed: ${ex.getMessage}\n")
        })
      }
    } else {
      statusArea.appendText("Not connected. Please connect to SSH first.\n")
    }
  }
}

