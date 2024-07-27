import algorithm.SearchAlg
import content.Tree
import global.Log
import javafx.application.Platform
import javafx.geometry.{Insets, Orientation}
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.{BorderPane, HBox}
import ssh.{RemoteFile, SSHManager}
import util.Downloads

import java.nio.file.{Files, Paths}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}

class ViewTab(sshManager: SSHManager) {
  private val fileTreeView = new TreeView[RemoteFile]()
  fileTreeView.setShowRoot(false)
  fileTreeView.setCellFactory(Tree.createRemoteFileCellFactory())

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

  private val toggleDownloadBtn = new Button("+Downloads")
  toggleDownloadBtn.setDisable(true)

  private val fileContentViewer = new FileContentViewer(800, 600)

  Platform.runLater(() => {
    updateFileTreeAsync()
  })

  // UIコンテンツの取得メソッド
  def getContent: BorderPane = {
    val refreshButton = new Button("Refresh")
    refreshButton.setOnAction(_ => updateFileTreeAsync())

    val searchButton = new Button("Search")
    searchButton.setOnAction(_ => performSearchAsync())

    toggleDownloadBtn.setOnAction(_ => toggleDownloadList())

    // UIレイアウトの設定
    val splitPane = new SplitPane()
    splitPane.setOrientation(Orientation.HORIZONTAL)
    splitPane.getItems.addAll(fileTreeView, contentArea)
    splitPane.setDividerPositions(0.3)

    val topPane = new HBox(10)
    topPane.setPadding(new Insets(10))
    topPane.getChildren.addAll(refreshButton, searchField, searchButton, showHiddenRadio, hideHiddenRadio, toggleDownloadBtn)

    val content = new BorderPane()
    content.setTop(topPane)
    content.setCenter(splitPane)

    content
  }

  // ファイル選択時のリスナー設定を更新
  fileTreeView.getSelectionModel.selectedItemProperty().addListener((_, _, newValue) => {
    if (newValue != null) {
      val file = newValue.getValue
      if (!file.isDirectory) {
        // 即座にファイル内容を表示
        viewFileContent(file)
        toggleDownloadBtn.setDisable(false)
        updateToggleButtonText(file)

        // 非同期でファイル情報を更新
        Future {
          updateFileInfoAsync(file)
        }
      } else {
        toggleDownloadBtn.setDisable(true)
        contentArea.setContent(null)
      }
    }
  })

  // ダウンロードリストの切り替えメソッド
  private def toggleDownloadList(): Unit = {
    val selectedItem = fileTreeView.getSelectionModel.getSelectedItem
    if (selectedItem != null && !selectedItem.getValue.isDirectory) {
      val remoteFile = selectedItem.getValue
      if (Downloads.getDownloadList.contains(remoteFile)) {
        Downloads.getDownloadList.remove(remoteFile)
        Log.dbg(s"Removed ${remoteFile.name} from download list")
      } else {
        Downloads.addToDownloadList(remoteFile)
      }
      updateToggleButtonText(remoteFile)
    }
  }

  // ファイル検索メソッド
  private def performSearchAsync(): Unit = {
    val searchTerm = searchField.getText.trim.toLowerCase
    if (searchTerm.isEmpty) {
      Log.err("Please enter a search term.")
      return
    }

    if (sshManager.isConnected) {
      Log.apt("Searching...")
      sshManager.withSSH { ssh =>
        SearchAlg.searchAsync(ssh, searchTerm, showHiddenRadio.isSelected).foreach { searchResults =>
          Platform.runLater(() => {
            val rootItem = new TreeItem[RemoteFile](RemoteFile("Search Results", "", isDirectory = true, depth = -1))
            searchResults.foreach { file =>
              addSearchResultToTree(rootItem, file)
            }
            fileTreeView.setRoot(rootItem)
            rootItem.setExpanded(true)
            Log.info(s"Found ${searchResults.length} results for '$searchTerm'")
          })
        }
      }
    } else {
      Log.warn("Not connected. Please connect to SSH first.")
    }
  }

  // ファイルツリーの非同期更新メソッド
  private def updateFileTreeAsync(): Unit = {
    if (sshManager.isConnected) {
      Log.apt("Refreshing file tree...")
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
              Log.info("Remote file tree updated.")
            })
          }
        }
      }.recover {
        case ex => Platform.runLater(() => {
          Log.err(s"Failed to refresh file tree: ${ex.getMessage}")
        })
      }
    } else {
      Log.warn("Not connected. Please connect to SSH first.")
    }
  }

  // ディレクトリツリーの作成メソッド
  private def createDirectoryTree(fileList: List[String], homeDir: String): TreeItem[RemoteFile] = {
    val root = new TreeItem(RemoteFile("~", homeDir, isDirectory = true, size = 0))
    root.setExpanded(true)

    fileList.foreach { path =>
      addPathToTree(root, path.substring(homeDir.length + 1), path, homeDir)
    }

    root
  }

  private def addSearchResultToTree(root: TreeItem[RemoteFile], file: RemoteFile): Unit = {
    val path = Paths.get(file.fullPath)
    val parentPath = path.getParent
    val relativePath = if (parentPath != null) parentPath.getFileName.toString + "/" + file.name else file.name

    val parts = relativePath.split("/")
    var current = root
    var currentDepth = root.getValue.depth

    parts.init.foreach { part =>
      currentDepth += 1
      current = current.getChildren.asScala.find(_.getValue.name == part) match {
        case Some(existing) => existing
        case None =>
          val newItem = new TreeItem(RemoteFile(part, "", isDirectory = true, depth = currentDepth))
          current.getChildren.add(newItem)
          newItem
      }
    }

    val fileItem = new TreeItem(file.copy(name = parts.last))
    current.getChildren.add(fileItem)
  }

  // ツリーにパスを追加するメソッド
  private def addPathToTree(root: TreeItem[RemoteFile], relativePath: String, fullPath: String, homeDir: String): Unit = {
    val parts = relativePath.split("/")
    var current = root

    parts.zipWithIndex.foreach { case (part, index) =>
      val isLast = index == parts.length - 1
      val isDirectory = !isLast || fullPath.endsWith("/")

      // 初期状態では最小限の情報でRemoteFileを作成
      val initialSize = if (isDirectory) 0 else -1L

      current = current.getChildren.asScala.find(_.getValue.name == part) match {
        case Some(existing) => existing
        case None =>
          val newPath = s"$homeDir/${parts.take(index + 1).mkString("/")}"
          val newItem = new TreeItem(RemoteFile(part, newPath, isDirectory, initialSize))
          current.getChildren.add(newItem)
          if (isDirectory) newItem.setExpanded(true)
          newItem
      }
    }
  }

  // ダウンロードボタンのテキスト更新メソッド
  private def updateToggleButtonText(file: RemoteFile): Unit = {
    if (Downloads.getDownloadList.contains(file)) {
      toggleDownloadBtn.setText("-Downloads")
    } else {
      toggleDownloadBtn.setText("+Downloads")
    }
  }

  // ファイル情報を非同期で更新するメソッド
  private def updateFileInfoAsync(file: RemoteFile): Unit = {
    Downloads.updateFileInfoAsync(file).onComplete {
      case Success(updatedFile) =>
        Platform.runLater(() => {
          // TreeViewのアイテムを更新
          val selectedItem = fileTreeView.getSelectionModel.getSelectedItem
          if (selectedItem != null && selectedItem.getValue == file) {
            selectedItem.setValue(updatedFile)
          }
        })
      case Failure(ex) =>
        Log.err(s"Failed to update file information: ${ex.getMessage}")
    }
  }

  // ファイル内容の表示メソッド（非同期処理）
  private def viewFileContent(file: RemoteFile): Unit = {
    if (sshManager.isConnected) {
      Future {
        sshManager.withSSH { ssh =>
          Try {
            val tempDir = Files.createTempDirectory("ssh-viewer")
            val tempFile = tempDir.resolve(file.name)
            ssh.get(file.fullPath, tempFile.toString)

            val contentNode: Node = fileContentViewer.viewContent(tempFile.toFile)

            Platform.runLater(() => {
              contentArea.setContent(contentNode)
              Log.info(s"Viewing file: ${file.name}")
            })

            Files.delete(tempFile)
            Files.delete(tempDir)
          }
        }
      }.recover {
        case ex => Platform.runLater(() => {
          Log.err(s"Failed to view file: ${ex.getMessage}")
        })
      }
    } else {
      Log.err("Not connected. Please connect to SSH first.")
    }
  }
}