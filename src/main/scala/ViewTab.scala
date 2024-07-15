import content.RemoteFile
import content.Tree.addPathToTree
import global.{Config, IO}
import javafx.application.Platform
import javafx.geometry.{Insets, Orientation}
import javafx.scene.Node
import javafx.scene.control.cell.TextFieldTreeCell
import javafx.scene.control.*
import javafx.scene.image.{Image, ImageView}
import javafx.scene.layout.{BorderPane, HBox}
import javafx.scene.text.Text
import javafx.util.StringConverter
import ssh.SSHManager
import content.Status

import java.nio.file.{Files, Paths}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.*
import scala.util.Try


class ViewTab(sshManager: SSHManager) {
  private val fileTreeView = new TreeView[RemoteFile]()
  fileTreeView.setShowRoot(false)
  fileTreeView.setCellFactory(_ => new TextFieldTreeCell[RemoteFile](new StringConverter[RemoteFile]() {
    override def toString(rf: RemoteFile): String = rf.name
    override def fromString(string: String): RemoteFile = null // Not used for this example
  }))

  private val contentArea = new ScrollPane()
  contentArea.setFitToWidth(true)
  contentArea.setFitToHeight(true)

  private val searchField = new TextField()
  searchField.setPromptText("Search files...")

  private val showHiddenToggleGroup = new ToggleGroup()
  private val showHiddenRadio = new RadioButton("Show Hidden")
  private val hideHiddenRadio = new RadioButton("Hide Hidden")
  showHiddenRadio.setToggleGroup(showHiddenToggleGroup)
  hideHiddenRadio.setToggleGroup(showHiddenToggleGroup)
  hideHiddenRadio.setSelected(true)

  private val saveButton = new Button("Save")
  saveButton.setDisable(true)

  def getContent: BorderPane = {
    val refreshButton = new Button("Refresh")
    refreshButton.setOnAction(_ => updateFileTreeAsync())

    val searchButton = new Button("Search")
    searchButton.setOnAction(_ => performSearchAsync())

    saveButton.setOnAction(_ => downloadSelectedFile())

    fileTreeView.getSelectionModel.selectedItemProperty().addListener((_, _, newValue) => {
      if (newValue != null && !newValue.getValue.isDirectory) {
        viewFileContent(newValue.getValue)
        saveButton.setDisable(false)
      } else {
        saveButton.setDisable(true)
      }
    })

    val splitPane = new SplitPane()
    splitPane.setOrientation(Orientation.HORIZONTAL)
    splitPane.getItems.addAll(fileTreeView, contentArea)
    splitPane.setDividerPositions(0.3)

    val topPane = new HBox(10)
    topPane.setPadding(new Insets(10))
    topPane.getChildren.addAll(refreshButton, searchField, searchButton, showHiddenRadio, hideHiddenRadio, saveButton)

    val content = new BorderPane()
    content.setTop(topPane)
    content.setCenter(splitPane)

    content
  }

  private def downloadSelectedFile(): Unit = {
    if (sshManager.isConnected) {
      val selectedItem = fileTreeView.getSelectionModel.getSelectedItem
      if (selectedItem != null && !selectedItem.getValue.isDirectory) {
        val remoteFile = selectedItem.getValue.fullPath
        val saveAsName = selectedItem.getValue.name
        val localPath = Paths.get(Config.dir.downloadsDir, saveAsName).toString

        sshManager.withSSH { ssh =>
          Try {
            ssh.get(remoteFile, localPath)
            Platform.runLater(() => {
              Status.appendText(s"File downloaded: $remoteFile -> $localPath")
            })
          }.recover {
            case ex => Platform.runLater(() => {
              Status.appendText(s"Download failed: ${ex.getMessage}")
            })
          }
        }.recover {
          case ex => Platform.runLater(() => {
            Status.appendText(s"SSH operation failed: ${ex.getMessage}")
          })
        }
      } else {
        Status.appendText("Please select a file to download.")
      }
    } else {
      Status.appendText("Not connected. Please connect to SSH first.")
    }
  }

  private def performSearchAsync(): Unit = {
    val searchTerm = searchField.getText.trim.toLowerCase
    if (searchTerm.isEmpty) {
      Status.appendText("Please enter a search term.\n")
      return
    }

    if (sshManager.isConnected) {
      Status.appendText("Searching...")
      Future {
        sshManager.withSSH { ssh =>
          Try {
            val homeDir = ssh.exec("echo $HOME").trim
            val showHidden = showHiddenRadio.isSelected
            val searchCommand = if (showHidden) {
              s"find $homeDir -iname '*$searchTerm*'"
            } else {
              s"find $homeDir -not -path '*/.*' -iname '*$searchTerm*'"
            }
            val searchResults = ssh.exec(searchCommand).split("\n").filter(_.nonEmpty)

            Platform.runLater(() => {
              val rootItem = new TreeItem[RemoteFile](RemoteFile("Search Results", homeDir, isDirectory = true))
              searchResults.foreach { path =>
                val relativePath = path.replace(homeDir, "").stripPrefix("/")
                addPathToTree(rootItem, relativePath, path, homeDir)
              }
              fileTreeView.setRoot(rootItem)
              Status.appendText(s"Found ${searchResults.length} results for '$searchTerm'")
            })
          }
        }
      }.recover {
        case ex => Platform.runLater(() => {
          Status.appendText(s"Search failed: ${ex.getMessage}")
        })
      }
    } else {
      Status.appendText("Not connected. Please connect to SSH first.")
    }
  }

  private def updateFileTreeAsync(): Unit = {
    if (sshManager.isConnected) {
      Status.appendText("Refreshing file tree...")
      Future {
        sshManager.withSSH { ssh =>
          Try {
            val homeDir = ssh.exec("echo $HOME").trim
            val showHidden = showHiddenRadio.isSelected
            val findCommand = if (showHidden) {
              s"find $homeDir"
            } else {
              s"find $homeDir -not -path '*/.*'"
            }
            val fileList = ssh.exec(findCommand).split("\n").filter(_.nonEmpty)
            val rootItem = new TreeItem[RemoteFile](RemoteFile("Root", homeDir, isDirectory = true))

            fileList.foreach { path =>
              val relativePath = path.replace(homeDir, "").stripPrefix("/")
              addPathToTree(rootItem, relativePath, path, homeDir)
            }

            Platform.runLater(() => {
              fileTreeView.setRoot(rootItem)
              Status.appendText("Remote file tree updated.")
            })
          }
        }
      }.recover {
        case ex => Platform.runLater(() => {
          Status.appendText(s"Failed to refresh file tree: ${ex.getMessage}")
        })
      }
    } else {
      Status.appendText("Not connected. Please connect to SSH first.")
    }
  }

  private def isImageFile(fileName: String): Boolean = {
    val imageExtensions = Set(".jpg", ".jpeg", ".png", ".gif", ".bmp")
    imageExtensions.exists(ext => fileName.toLowerCase.endsWith(ext))
  }

  private def isTextFile(file: java.nio.file.Path): Boolean = {
    val knownTextExtensions = Set(".txt", ".py", ".html", ".css", ".js", ".json", ".xml", ".md", ".scala", ".java", ".c", ".cpp", ".h", ".sh", ".bat", ".csv")

    if (knownTextExtensions.exists(ext => file.getFileName.toString.toLowerCase.endsWith(ext))) {
      return true
    }

    Try {
      val bytes = Files.readAllBytes(file)
      val n = Math.min(bytes.length, 1000) // Check only first 1000 bytes
      val numOfNonPrintable = bytes.take(n).count(b => b < 32 && b != 9 && b != 10 && b != 13)
      numOfNonPrintable.toDouble / n < 0.05 // If less than 5% non-printable characters, consider it text
    }.getOrElse(false)
  }

  private def viewFileContent(file: RemoteFile): Unit = {
    if (sshManager.isConnected) {
      sshManager.withSSH { ssh =>
        Try {
          val tempDir = Files.createTempDirectory("ssh-viewer")
          val tempFile = tempDir.resolve(file.name)
          ssh.get(file.fullPath, tempFile.toString)

          val contentNode = if (isImageFile(file.name)) {
            val image = new Image(tempFile.toUri.toString)
            val imageView = new ImageView(image)
            imageView.setPreserveRatio(true)
            imageView.setFitWidth(contentArea.getWidth - 20)
            imageView
          } else if (isTextFile(tempFile)) {
            val content = new String(Files.readAllBytes(tempFile))
            val textArea = new TextArea(content)
            textArea.setEditable(false)
            textArea.setWrapText(true)
            textArea
          } else {
            val text = new Text(s"This is a binary file: ${file.name}\nFile size: ${Files.size(tempFile)} bytes")
            text
          }

          Platform.runLater(() => {
            contentArea.setContent(contentNode)
            Status.appendText(s"Viewing file: ${file.name}")
          })

          Files.delete(tempFile)
          Files.delete(tempDir)
        }.recover {
          case ex => Platform.runLater(() => {
            Status.appendText(s"Failed to view file: ${ex.getMessage}")
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