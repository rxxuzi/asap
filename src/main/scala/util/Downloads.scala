package util

import global.{Config, Log}
import javafx.collections.{FXCollections, ObservableList}
import ssh.{RemoteFile, SSHManager}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
 * <h1>Downloads</h1>
 * Utility object for managing downloads via SSH.
 */
object Downloads {
  private val downloadList: ObservableList[RemoteFile] = FXCollections.observableArrayList[RemoteFile]()
  private var sshManager: SSHManager = _
  private var downloadDir = Config.dir.downloadsDir

  /**
   * Sets the SSH manager used for download operations.
   *
   * @param manager the SSHManager instance
   */
  def setSSHManager(manager: SSHManager): Unit = {
    sshManager = manager
  }

  /**
   * Gets the current SSH manager.
   *
   * @return the SSHManager instance
   */
  def getSSHManager: SSHManager = sshManager

  /**
   * Adds a file to the download list if it is not already present.
   *
   * @param file the RemoteFile to add
   */
  def addToDownloadList(file: RemoteFile): Unit = {
    if (!downloadList.contains(file)) {
      downloadList.add(file)
      Log.info(s"Added ${file.name} to download list")
      updateFileInfoAsync(file)
    } else {
      Log.info(s"${file.name} is already in the download list")
    }
  }

  /**
   * Gets the current download list.
   *
   * @return an ObservableList of RemoteFile
   */
  def getDownloadList: ObservableList[RemoteFile] = downloadList

  /**
   * Clears the download list.
   */
  def clearDownloadList(): Unit = {
    downloadList.clear()
    Log.info("Cleared download list")
  }

  /**
   * Updates the file information asynchronously using the SSH manager.
   *
   * @param file the RemoteFile to update
   * @return a Future containing the updated RemoteFile
   */
  def updateFileInfoAsync(file: RemoteFile): Future[RemoteFile] = {
    Future {
      sshManager.withSSH(ssh => file.update(ssh)) match {
        case Success(updatedFile) =>
          val index = downloadList.indexOf(file)
          if (index != -1) {
            downloadList.set(index, updatedFile)
          }
          updatedFile
        case Failure(exception) => file
      }
    }
  }

  /**
   * Formats a file size in bytes into a human-readable string with appropriate units.
   *
   * @param size the size in bytes
   * @return a formatted string representing the size
   */
  def formatFileSize(size: Long): String = {
    val units = Array("B", "KB", "MB", "GB", "TB")
    var value = size.toDouble
    var unitIndex = 0
    while (value > 1024 && unitIndex < units.length - 1) {
      value /= 1024
      unitIndex += 1
    }
    f"$value%.1f ${units(unitIndex)}"
  }

  /**
   * Gets a string containing file information, including size.
   *
   * @param file the RemoteFile to get information for
   * @return a string representing the file information
   */
  def getFileInfoString(file: RemoteFile): String = {
    val sizeInfo = if (file.size >= 0) s"Size: ${formatFileSize(file.size)}" else "Size: Unknown"
    s"${file.name} | $sizeInfo"
  }
}
