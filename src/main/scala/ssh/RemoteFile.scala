package ssh

import java.time.Instant

/**
 * Represents a file or directory on a remote SSH server.
 *
 * This class encapsulates information about a remote file or directory,
 * including its name, path, size, and modification time. It also provides
 * methods for updating and retrieving file information.
 *
 * @param name The name of the file or directory.
 * @param fullPath The full path of the file or directory on the remote server.
 * @param isDirectory Indicates whether this is a directory (true) or a file (false).
 * @param size The size of the file in bytes, or -1 if unknown.
 * @param modifiedTime The last modification time of the file, if available.
 * @param depth The depth of the file in the directory structure.
 * @param member The member or owner of the file.
 */
case class RemoteFile(
                       name: String,
                       fullPath: String,
                       isDirectory: Boolean,
                       size: Long = -1,
                       modifiedTime: Option[Instant] = None,
                       depth: Int = 0,
                       member: String = "~"
                     ) {

  /**
   * Returns a string representation of the RemoteFile.
   *
   * @return A formatted string containing file information.
   */
  override def toString: String =
    s"""
       |${if (isDirectory) "[d]" else "[f]"} ($depth) [$member] $fullPath
       |""".stripMargin

  /**
   * Updates the RemoteFile with complete information.
   *
   * This method uses the provided SSH instance to fetch and update
   * file information such as size, modified time, and directory status.
   *
   * @param ssh The SSH instance to use for executing remote commands.
   * @return An updated RemoteFile object with complete information.
   */
  def update(ssh: SSH): RemoteFile = {
    if (this.size == -1 || this.modifiedTime.isEmpty) {
      val updatedSize = getFileSize(ssh)
      val updatedIsDirectory = isDirectory(ssh)
      val updatedModifiedTime = getModifiedTime(ssh)
      val homeDir = ssh.exec("echo $HOME").trim
      val updatedDepth = this.fullPath.count(_ == '/') - homeDir.count(_ == '/')
      this.copy(
        size = updatedSize,
        isDirectory = updatedIsDirectory,
        modifiedTime = updatedModifiedTime,
        depth = updatedDepth
      )
    } else {
      this
    }
  }

  /**
   * Gets the size of the file on the remote server.
   *
   * @param ssh The SSH instance to use for executing remote commands.
   * @return The size of the file in bytes, or -1 if the size couldn't be determined.
   */
  def getFileSize(ssh: SSH): Long = {
    ssh.exec(s"stat -c %s '${this.fullPath}'").trim.toLongOption.getOrElse(-1L)
  }

  /**
   * Checks if the path on the remote server is a directory.
   *
   * @param ssh The SSH instance to use for executing remote commands.
   * @return true if the path is a directory, false otherwise.
   */
  def isDirectory(ssh: SSH): Boolean = {
    ssh.exec(s"test -d '${this.fullPath}' && echo 'true' || echo 'false'").trim.toBoolean
  }

  /**
   * Gets the modified time of the file on the remote server.
   *
   * @param ssh The SSH instance to use for executing remote commands.
   * @return The modified time as an Option[Instant], or None if it couldn't be determined.
   */
  def getModifiedTime(ssh: SSH): Option[Instant] = {
    ssh.exec(s"stat -c %Y '${this.fullPath}'").trim.toLongOption.map(Instant.ofEpochSecond)
  }
}