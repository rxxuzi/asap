import global.{Config, Log}
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.geometry.{Insets, Pos}
import javafx.scene.control
import javafx.scene.control.*
import javafx.scene.layout.{HBox, Priority, VBox}
import ssh.RemoteFile
import util.Downloads

import java.io.File
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success}

class DownloadTab {
  private val listView = new ListView[RemoteFile](Downloads.getDownloadList)
  private val sizeInfoLabel = new Label("Total size: 0 B")

  private val saveButton = new Button("Save")
  private val saveAllButton = new Button("All")
  private val removeButton = new Button("Remove")
  private val clearButton = new Button("Clear")

  def getContent: VBox = {
    listView.getSelectionModel.setSelectionMode(SelectionMode.MULTIPLE)

    saveButton.setOnAction(_ => downloadSelectedFiles())
    saveAllButton.setOnAction(_ => downloadAllFiles())
    removeButton.setOnAction(_ => removeSelectedFiles())
    clearButton.setOnAction(_ => Downloads.clearDownloadList())

    val buttonBox = new HBox(10, saveButton, saveAllButton, removeButton, clearButton)
    buttonBox.setAlignment(Pos.CENTER_LEFT)

    val topBox = new VBox(10, sizeInfoLabel)
    topBox.setPadding(new Insets(10))

    val mainBox = new VBox(10, topBox, listView, buttonBox)
    mainBox.setPadding(new Insets(10))
    VBox.setVgrow(listView, Priority.ALWAYS)

    setupListViewCellFactory()
    setupSizeInfoBinding()

    mainBox
  }

  private def setupListViewCellFactory(): Unit = {
    listView.setCellFactory(_ => new ListCell[RemoteFile] {
      override def updateItem(item: RemoteFile, empty: Boolean): Unit = {
        super.updateItem(item, empty)
        if (empty || item == null) setText(null)
        else setText(s"${item.name} (${Downloads.formatFileSize(item.size)})")
      }
    })
  }

  private def setupSizeInfoBinding(): Unit = {
    val selectedItemsProperty = listView.getSelectionModel.getSelectedItems
    sizeInfoLabel.textProperty().bind(
      Bindings.createStringBinding(() => {
        val totalSize = selectedItemsProperty.asScala.map(_.size).sum
        s"Total size: ${Downloads.formatFileSize(totalSize)}"
      }, selectedItemsProperty)
    )
  }

  private def downloadSelectedFiles(): Unit = {
    val selectedFiles = listView.getSelectionModel.getSelectedItems.asScala.toList
    if (selectedFiles.nonEmpty) {
      downloadFiles(selectedFiles)
    } else {
      Log.dbg("No files selected for download")
    }
  }

  private def downloadAllFiles(): Unit = {
    val allFiles = Downloads.getDownloadList.asScala.toList
    if (allFiles.nonEmpty) {
      downloadFiles(allFiles)
    } else {
      Log.dbg("Download list is empty")
    }
  }

  private def downloadFiles(files: List[RemoteFile]): Unit = {
    val downloadDir = new File(Config.dir.downloadsDir)
    if (!downloadDir.exists()) {
      downloadDir.mkdirs()
    }

    disableButtons(true)

    Future {
      files.zipWithIndex.foreach { case (file, index) =>
        Downloads.getSSHManager.withSSH { ssh =>
          val localPath = new File(downloadDir, file.name).getAbsolutePath
          ssh.get(file, localPath)
          Platform.runLater(() => {
            // 進捗更新のコードをここに追加する場合
          })
        }
      }
    }.onComplete {
      case Success(_) => Platform.runLater(() => {
        disableButtons(false)
        Log.info("All selected files downloaded successfully")
      })
      case Failure(ex) => Platform.runLater(() => {
        disableButtons(false)
        Log.err(s"Download failed: ${ex.getMessage}")
      })
    }
  }

  private def removeSelectedFiles(): Unit = {
    val selectedFiles = listView.getSelectionModel.getSelectedItems
    Downloads.getDownloadList.removeAll(selectedFiles)
    Log.dbg(s"Removed ${selectedFiles.size()} files from download list")
  }

  private def disableButtons(disable: Boolean): Unit = {
    saveButton.setDisable(disable)
    saveAllButton.setDisable(disable)
    removeButton.setDisable(disable)
    clearButton.setDisable(disable)
  }
}