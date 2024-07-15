import content.{RemoteFile, Tree}
import javafx.application.Platform
import javafx.geometry.{Insets, Orientation}
import javafx.scene.control.*
import javafx.scene.control.cell.TextFieldTreeCell
import javafx.scene.layout.{HBox, Priority, VBox}
import javafx.stage.FileChooser
import javafx.util.StringConverter
import ssh.SSHManager
import content.Status

import java.io.File
import scala.jdk.CollectionConverters.*
import scala.util.Try

class SendTab(sshManager: SSHManager) {
  private val fileTreeView = new TreeView[RemoteFile]()
  fileTreeView.setShowRoot(false)
  fileTreeView.setCellFactory(_ => new TextFieldTreeCell[RemoteFile](new StringConverter[RemoteFile]() {
    override def toString(rf: RemoteFile): String = rf.name

    override def fromString(string: String): RemoteFile = null // Not used for this example
  }))

  private val showAllFilesRadio = new RadioButton("Show All Files")
  private val showVisibleFilesRadio = new RadioButton("Show Visible Files Only")
  private val toggleGroup = new ToggleGroup()
  showAllFilesRadio.setToggleGroup(toggleGroup)
  showVisibleFilesRadio.setToggleGroup(toggleGroup)
  showVisibleFilesRadio.setSelected(true)

  private val localFilePathField = new TextField()
  private val remoteFilePathField = new TextField()
  localFilePathField.setPrefWidth(300)
  remoteFilePathField.setPrefWidth(300)

  def getContent: SplitPane = {
    localFilePathField.setPromptText("Local file path")
    remoteFilePathField.setPromptText("Remote directory path")

    val chooseFileButton = new Button("Choose File")
    val sendButton = new Button("Send File")
    val refreshButton = new Button("Refresh")

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
        val localFile = new File(localFilePathField.getText)
        val remotePath = remoteFilePathField.getText
        if (localFile.exists() && remotePath.nonEmpty) {
          sshManager.withSSH { ssh =>
            val remoteFilePath = s"$remotePath/${localFile.getName}"
            ssh.send(localFile, remoteFilePath)
            Status.appendText(s"File sent: ${localFile.getAbsolutePath} -> $remoteFilePath")
            updateFileTree()
          }.recover {
            case ex => Status.appendText(s"Send failed: ${ex.getMessage}")
          }
        } else {
          Status.appendText("Please select a valid local file and remote directory.")
        }
      } else {
        Status.appendText("Not connected. Please connect to SSH first.")
      }
    })

    refreshButton.setOnAction(_ => updateFileTree())

    toggleGroup.selectedToggleProperty().addListener((_, _, newValue) => {
      if (newValue != null) {
        updateFileTree()
      }
    })

    fileTreeView.getSelectionModel.selectedItemProperty().addListener((_, _, newValue) => {
      if (newValue != null) {
        remoteFilePathField.setText(newValue.getValue.fullPath)
      }
    })

    val topPane = new VBox(10)
    topPane.setPadding(new Insets(10))
    topPane.getChildren.addAll(
      new HBox(10, new Label("Local Path:"), localFilePathField, chooseFileButton),
      new HBox(10, new Label("Remote Path:"), remoteFilePathField),
      new HBox(10, sendButton)
    )

    val leftPane = new VBox(10)
    leftPane.setPadding(new Insets(10))
    leftPane.getChildren.addAll(
      new HBox(10, new Label("Remote Directories:"), refreshButton),
      new HBox(10, showAllFilesRadio, showVisibleFilesRadio),
      fileTreeView
    )
    VBox.setVgrow(fileTreeView, Priority.ALWAYS)


    val bottomPane = new SplitPane()
    bottomPane.setOrientation(Orientation.HORIZONTAL)
    bottomPane.getItems.addAll(leftPane)
    bottomPane.setDividerPositions(0.6)

    val mainSplitPane = new SplitPane()
    mainSplitPane.setOrientation(Orientation.VERTICAL)
    mainSplitPane.getItems.addAll(topPane, bottomPane)
    mainSplitPane.setDividerPositions(0.3)

    updateFileTree()

    mainSplitPane
  }

  private def updateFileTree(): Unit = {
    if (sshManager.isConnected) {
      sshManager.withSSH { ssh =>
        Try {
          val homeDir = ssh.exec("echo $HOME").trim
          val showAllFiles = showAllFilesRadio.isSelected
          val fileList = ssh.exec(s"find $homeDir -type d -o -type f").split("\n").filter(_.nonEmpty)
          val rootItem = new TreeItem[RemoteFile](RemoteFile("~", homeDir, isDirectory = true))
          rootItem.setExpanded(true)

          fileList.foreach { path =>
            val relativePath = path.replace(homeDir, "").stripPrefix("/")
            if (showAllFiles || !relativePath.split("/").exists(_.startsWith("."))) {
              Tree.addPathToTree(rootItem, relativePath, path, homeDir)
            }
          }

          Platform.runLater(() => {
            fileTreeView.setRoot(rootItem)
            Status.appendText("Remote file tree updated.")
          })
        }.recover {
          case ex => Platform.runLater(() => {
            Status.appendText(s"Failed to list remote files: ${ex.getMessage}")
          })
        }
      }.recover {
        case ex => Platform.runLater(() => {
          Status.appendText(s"SSH operation failed: ${ex.getMessage}")
        })
      }
    } else {
      Status.appendText("Not connected. Please connect to SSH first.")
    }
  }
}