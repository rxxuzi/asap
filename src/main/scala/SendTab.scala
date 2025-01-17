import content.{SelectedWindow, Tree}
import global.Log
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.geometry.{Insets, Orientation}
import javafx.scene.control.*
import javafx.scene.layout.{HBox, Priority, VBox}
import javafx.stage.FileChooser
import ssh.{RemoteFile, SSHManager}

import java.io.File
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}

final class SendTab(sshManager: SSHManager)(implicit ec: ExecutionContext) {
  private val maxSendFiles = 20
  private val fileTreeView = new TreeView[RemoteFile]()
  fileTreeView.setShowRoot(false)
  fileTreeView.setCellFactory(Tree.createRemoteFileCellFactory())

  private val showAllFilesRadio = new RadioButton("Show All Files")
  private val showVisibleFilesRadio = new RadioButton("Show Visible Files Only")
  private val toggleGroup = new ToggleGroup()
  showAllFilesRadio.setToggleGroup(toggleGroup)
  showVisibleFilesRadio.setToggleGroup(toggleGroup)
  showVisibleFilesRadio.setSelected(true)

  private val localFilesList = FXCollections.observableArrayList[File]()
  private val localFilesListView = new ListView[File](localFilesList)
  private val remoteFilePathField = new TextField()
  remoteFilePathField.setPrefWidth(300)

  private val selectedFilesLabel = new Label("Selected Files:")
  private val selectedFilesButton = new Button("View Selected Files")

  private var selectedWindow: SelectedWindow = _

  def getContent: SplitPane = {
    remoteFilePathField.setPromptText("Remote directory path")

    val chooseFilesButton = new Button("Choose Files")
    val sendButton = new Button("Send Files")
    val refreshButton = new Button("Refresh")
    val clearButton = new Button("Clear All")

    chooseFilesButton.setOnAction(_ => {
      val fileChooser = new FileChooser()
      fileChooser.setTitle("Select Files")
      val selectedFiles = fileChooser.showOpenMultipleDialog(null)
      if (selectedFiles != null) {
        val remainingSlots = maxSendFiles - localFilesList.size()
        val filesToAdd = selectedFiles.asScala.take(remainingSlots)
        localFilesList.addAll(filesToAdd.asJava)
        updateSelectedFilesButton()

        if (selectedFiles.size() > remainingSlots) {
          Log.warn(s"Only $remainingSlots files were added. Maximum of $maxSendFiles files allowed.")
        }
      }
    })

    selectedFilesButton.setOnAction(_ => {
      if (selectedWindow == null || !selectedWindow.isShowing) {
        selectedWindow = new SelectedWindow(localFilesList, files => {
          localFilesList.clear()
          localFilesList.addAll(files)
          updateSelectedFilesButton()
          Log.dbg("Update Selected Files : " + files.toString)
        })
        selectedWindow.show()
      } else {
        selectedWindow.toFront()
      }
    })

    sendButton.setOnAction(_ => {
      if (sshManager.isConnected) {
        val remotePath = remoteFilePathField.getText
        if (!localFilesList.isEmpty && remotePath.nonEmpty) {
          sendButton.setDisable(true)
          Future {
            localFilesList.asScala.foreach { localFile =>
              sshManager.withSSH { ssh =>
                val remoteFilePath = s"$remotePath/${localFile.getName}"
                ssh.send(localFile, remoteFilePath)
                Log.info(s"File sent: ${localFile.getAbsolutePath} -> $remoteFilePath")
              }
            }
            updateFileTree()
          }.onComplete {
            case Success(_) => Platform.runLater(() => {
              sendButton.setDisable(false)
            })
            case Failure(ex) => Platform.runLater(() => {
              sendButton.setDisable(false)
              Log.err(s"Failed to send files: ${ex.getMessage}")
            })
          }
        } else {
          Log.warn("Please select valid local files and a remote directory.")
        }
      } else {
        Log.err("Not connected. Please connect to SSH first.")
      }
    })

    refreshButton.setOnAction(_ => {
      refreshButton.setDisable(true)
      Future {
        updateFileTree()
      }.onComplete {
        case Success(_) => Platform.runLater(() => refreshButton.setDisable(false))
        case Failure(ex) => Platform.runLater(() => {
          refreshButton.setDisable(false)
          Log.err(s"Refresh failed: ${ex.getMessage}")
        })
      }
    })

    clearButton.setOnAction(_ => {
      localFilesList.clear()
      updateSelectedFilesButton()
    })

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

    localFilesListView.setCellFactory(_ => new ListCell[File] {
      override def updateItem(item: File, empty: Boolean): Unit = {
        super.updateItem(item, empty)
        if (empty || item == null) {
          setText(null)
        } else {
          setText(item.getName)
        }
      }
    })

    val topPane = new VBox(10)
    topPane.setPadding(new Insets(10))
    topPane.getChildren.addAll(
      new HBox(10, new Label("Local Files:"), chooseFilesButton, clearButton),
      new HBox(10, selectedFilesLabel, selectedFilesButton),
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

    val mainSplitPane = new SplitPane()
    mainSplitPane.setOrientation(Orientation.VERTICAL)
    mainSplitPane.getItems.addAll(topPane, leftPane)
    mainSplitPane.setDividerPositions(0.3)

    updateSelectedFilesButton()

    mainSplitPane
  }

  private def updateSelectedFilesButton(): Unit = {
    val count = localFilesList.size()
    selectedFilesButton.setText(s"View Selected Files ($count)")
  }

  private def updateFileTree(): Unit = {
    if (sshManager.isConnected) {
      sshManager.withSSH { ssh =>
        Try {
          val homeDir = ssh.exec("echo $HOME").trim
          val showAllFiles = showAllFilesRadio.isSelected
          val findCommand = if (showAllFiles) {
            s"find $homeDir -type d"
          } else {
            s"""find $homeDir -type d -not -path '*/\\.*'"""
          }
          val fileList = ssh.exec(findCommand).split("\n").filter(_.nonEmpty).toList
          val treeView = Tree.createDirectoryOnlyTree(fileList, homeDir)
          Platform.runLater(() => {
            fileTreeView.setRoot(treeView.getRoot)
            fileTreeView.setShowRoot(true)
            Log.info("Remote directory tree updated.")
          })
        }.recover {
          case ex => Platform.runLater(() => {
            Log.err(s"Failed to list remote directories: ${ex.getMessage}")
          })
        }
      }.recover {
        case ex => Platform.runLater(() => {
          Log.err(s"SSH operation failed: ${ex.getMessage}")
        })
      }
    } else {
      Log.err("Not connected. Please connect to SSH first.")
    }
  }
}