import command.{BuildCommand, SortBy}
import content.{RemoteFileContentViewer, Tree}
import global.Log
import javafx.application.Platform
import javafx.geometry.{Insets, Orientation}
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.{BorderPane, HBox}
import ssh.{RemoteFile, SSHManager}
import util.Downloads

import java.nio.file.Paths
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}

final class ViewTab(sshManager: SSHManager) {
  private val fileTreeView = new TreeView[RemoteFile]()
  fileTreeView.setShowRoot(false)
  fileTreeView.setCellFactory(Tree.createRemoteFileCellFactory())

  private val contentArea = new ScrollPane()
  contentArea.setFitToWidth(true)
  contentArea.setFitToHeight(true)

  private val searchField = new TextField()
  searchField.setPromptText("Search files...")

  private val sortComboBox = new ComboBox[SortBy]()
  sortComboBox.getItems.addAll(SortBy.values: _*)
  sortComboBox.setValue(SortBy.NameAsc)
  sortComboBox.setOnAction(_ => updateFileTreeAsync())

  private val showHiddenToggleGroup = new ToggleGroup()
  private val showHiddenRadio = new RadioButton("Show Hidden")
  private val hideHiddenRadio = new RadioButton("Hide Hidden")
  showHiddenRadio.setToggleGroup(showHiddenToggleGroup)
  hideHiddenRadio.setToggleGroup(showHiddenToggleGroup)
  hideHiddenRadio.setSelected(true)

  private val toggleDownloadBtn = new Button("+Downloads")
  toggleDownloadBtn.setDisable(true)

  private var remoteFileContentViewer: Option[RemoteFileContentViewer] = None

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
    topPane.getChildren.addAll(refreshButton, searchField, searchButton, sortComboBox, showHiddenRadio, hideHiddenRadio, toggleDownloadBtn)

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
        // 非同期でファイル情報を更新
        updateFileInfoAsync(file).onComplete {
          case Success(updatedFile) =>
            Platform.runLater(() => {
              newValue.setValue(updatedFile)
              viewFileContent(updatedFile)
              toggleDownloadBtn.setDisable(false)
              updateToggleButtonText(updatedFile)
            })
          case Failure(ex) =>
            Log.err(s"Failed to update file information: ${ex.getMessage}")
        }
      } else {
        toggleDownloadBtn.setDisable(true)
        contentArea.setContent(null)
      }
    }
  })

  // ファイル情報を非同期で更新するメソッド
  private def updateFileInfoAsync(file: RemoteFile): Future[RemoteFile] = {
    Future {
      sshManager.withSSH { ssh =>
        val sizeResult = Try {
          ssh.exec(s"wc -c '${file.fullPath}' | awk '{print $$1}'").trim.toLongOption.getOrElse(-1L)
        }
        sizeResult match {
          case Success(size) => file.copy(size = size)
          case Failure(ex) =>
            Log.err(s"Failed to get file size for ${file.name}: ${ex.getMessage}")
            file // Return the original file if size retrieval fails
        }
      }
    }.flatMap {
      case Success(updatedFile) => Future.successful(updatedFile)
      case Failure(ex) =>
        Log.err(s"Failed to update file info for ${file.name}: ${ex.getMessage}")
        Future.successful(file) // Return the original file if the update fails
    }
  }

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
    val searchTerm = searchField.getText.trim
    if (searchTerm.isEmpty) {
      Log.err("Please enter a search term.")
      return
    }

    if (sshManager.isConnected) {
      Log.apt("Searching...")
      sshManager.withSSH { ssh =>
        val hidden = showHiddenRadio.isSelected
        val sortBy = sortComboBox.getValue
        val searchCmd = BuildCommand.search(hidden, sortBy, searchTerm)

        Future {
          Try {
            val result = ssh.exec(searchCmd)
            val homeDir = ssh.exec("echo $HOME").trim
            val searchResults = result.split("\n").filter(_.nonEmpty).map { path =>
              val isDirectory = path.endsWith("/")
              val name = Paths.get(path).getFileName.toString
              RemoteFile(name = name, fullPath = path, isDirectory = isDirectory)
            }.toList

            Platform.runLater(() => {
              val rootItem = new TreeItem[RemoteFile](RemoteFile("Search Results", "", isDirectory = true, depth = -1))
              searchResults.foreach { file =>
                addSearchResultToTree(rootItem, file, homeDir)
              }
              fileTreeView.setRoot(rootItem)
              rootItem.setExpanded(true)
              rootItem.getChildren.forEach(_.setExpanded(false))
              val validResultCount = searchResults.size
              Log.info(s"Found $validResultCount valid results for '$searchTerm'")
            })
          }
        }.recover {
          case ex: Exception =>
            Platform.runLater(() => {
              Log.err(s"Search failed: ${ex.getMessage}")
            })
        }
      }
    } else {
      Log.warn("Not connected. Please connect to SSH first.")
    }
  }

  private def addSearchResultToTree(root: TreeItem[RemoteFile], file: RemoteFile, homeDir: String): Unit = {
    val relativePath = file.fullPath.replaceFirst(s"^$homeDir/", "")
    val parts = relativePath.split("/")
    var current = root
    parts.init.foreach { part =>
      current = current.getChildren.asScala.find(_.getValue.name == part) match {
        case Some(existing) => existing
        case None =>
          val newItem = new TreeItem(RemoteFile(part, s"$homeDir/${parts.take(parts.indexOf(part) + 1).mkString("/")}", isDirectory = true))
          current.getChildren.add(newItem)
          newItem
      }
    }
    val fileItem = new TreeItem(file)
    current.getChildren.add(fileItem)
  }

  private def countValidItems(item: TreeItem[RemoteFile]): Int = {
    1 + item.getChildren.asScala.map(countValidItems).sum
  }

  // ファイルツリーの非同期更新メソッド
  private def updateFileTreeAsync(): Unit = {
    searchField.setText("")
    if (sshManager.isConnected) {
      Log.apt("Refreshing file tree...")
      Future {
        sshManager.withSSH { ssh =>
          Try {
            val homeDir = ssh.exec("echo $HOME").trim
            val showHidden = showHiddenRadio.isSelected
            val sortBy = sortComboBox.getValue
            val findCommand = BuildCommand.sort(showHidden, sortBy)
            val fileList = ssh.exec(findCommand).split("\n").filter(_.nonEmpty).map(_.trim)
            val rootItem = new TreeItem[RemoteFile](RemoteFile("Root", homeDir, isDirectory = true))

            fileList.foreach { path =>
              if (path.nonEmpty && path != homeDir) { // 空のパスとホームディレクトリ自体をスキップ
                val relativePath = path.replace(homeDir, "").stripPrefix("/")
                if (relativePath.nonEmpty) { // 相対パスが空でない場合のみ追加
                  addPathToTree(rootItem, relativePath, path, homeDir)
                }
              }
            }

            Platform.runLater(() => {
              fileTreeView.setRoot(rootItem)
              // ルートアイテムとその直下のアイテムのみを展開
              rootItem.setExpanded(true)
              rootItem.getChildren.forEach(_.setExpanded(false))
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

  // ツリーにパスを追加するメソッド
  private def addPathToTree(root: TreeItem[RemoteFile], relativePath: String, fullPath: String, homeDir: String): Unit = {
    val parts = relativePath.split("/")
    var current = root
    var currentDepth = root.getValue.depth
    var currentParent = root.getValue.fullPath
    var currentMember = "~"

    parts.zipWithIndex.foreach { case (part, index) =>
      val isLast = index == parts.length - 1
      val isDirectory = !isLast || fullPath.endsWith("/")
      currentDepth += 1

      // RemoteFile の初期状態（最小限の情報）
      val initialSize = if (isDirectory) 0 else -1L
      val newPath = s"$homeDir/${parts.take(index + 1).mkString("/")}"

      current = current.getChildren.asScala.find(_.getValue.name == part) match {
        case Some(existing) =>
          // 既存のアイテムを新しい depth と member で更新
          val updatedFile = existing.getValue.copy(depth = currentDepth, member = currentMember)
          existing.setValue(updatedFile)
          existing
        case None =>
          val newItem = new TreeItem(RemoteFile(
            name = part,
            fullPath = newPath,
            isDirectory = isDirectory,
            size = initialSize,
            depth = currentDepth,
            member = currentMember
          ))
          current.getChildren.add(newItem)
          // 新しく追加されたアイテムは閉じた状態にする
          newItem.setExpanded(false)
          newItem
      }

      // 次の繰り返しのために currentMember を更新
      if (isDirectory) {
        currentMember = part
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

  // ファイル内容の表示メソッド（非同期処理）
  private def viewFileContent(file: RemoteFile): Unit = {
    if (sshManager.isConnected) {
      sshManager.withSSH { ssh =>
        if (remoteFileContentViewer.isEmpty) {
          remoteFileContentViewer = Some(new RemoteFileContentViewer(800, 600, ssh))
        }

        remoteFileContentViewer.foreach { viewer =>
          viewer.viewContent(file).onComplete {
            case Success(contentNode) =>
              Platform.runLater(() => {
                contentArea.setContent(contentNode)
                Log.info(s"Viewing file: ${file.name}")
              })
            case Failure(ex) =>
              Platform.runLater(() => {
                Log.err(s"Failed to view file: ${ex.getMessage}")
              })
          }
        }
      }
    } else {
      Log.err("Not connected. Please connect to SSH first.")
    }
  }
}