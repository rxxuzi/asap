package ssh

import org.apache.sshd.client.SshClient
import org.apache.sshd.client.channel.ClientChannelEvent
import org.apache.sshd.client.session.ClientSession
import org.apache.sshd.sftp.client.{SftpClient, SftpClientFactory}

import java.io.*
import java.nio.file.{Files, Paths, StandardCopyOption}
import java.util
import java.util.EnumSet
import java.util.concurrent.{Executors, TimeUnit}
import scala.jdk.CollectionConverters.*
import scala.util.Using

/**
 * <h1>SSH</h1>
 * Represents an SSH connection to a remote server.
 *
 * This class provides methods for establishing an SSH connection,
 * executing commands, transferring files, and managing the current working directory.
 * It also includes a keep-alive feature to prevent automatic session timeout.
 *
 * @constructor Creates a new SSH connection with the specified parameters.
 * @param host The hostname or IP address of the remote server.
 * @param port The port number for the SSH connection (usually 22).
 * @param user The username for authentication.
 * @param password The password for authentication.
 */
class SSH(val host: String, val port: Int, val user: String, val password: String) extends SSHTrait {
  val userHost: String = s"$user@$host"
  private var session: Option[ClientSession] = None
  private val client: SshClient = SshClient.setUpDefaultClient()
  private var currentDirectory: String = "/"

  private val executorService = Executors.newSingleThreadScheduledExecutor()

  /**
   * Creates a new SSH connection using configuration from a JSON file.
   *
   * @param path The path to the JSON configuration file.
   */
  def this(path: String) = {
    this(SSHJson(path).host, SSHJson(path).port, SSHJson(path).user, SSHJson(path).pass)
  }

  /**
   * Opens the SSH connection to the remote server.
   *
   * This method establishes the connection, authenticates the user,
   * and sets the initial working directory. If keep-alive is enabled,
   * it also starts the keep-alive mechanism.
   */
  override def open(): Unit = {
    client.start()
    connectAndAuthenticate()
    currentDirectory = exec("pwd").trim
    if (SSH.keepAliveEnabled) {
      startKeepAlive()
    }
  }

  /**
   * Closes the SSH connection and releases resources.
   *
   * This method should be called when the SSH connection is no longer needed.
   * It also stops the keep-alive mechanism if it was enabled.
   */
  override def close(): Unit = {
    stopKeepAlive()
    session.foreach(_.close())
    session = None
    client.stop()
    executorService.shutdown()
  }

  /**
   * Retrieves information about the current SSH session.
   *
   * @return A string containing the session information if connected,
   *         or a message indicating no active session.
   */
  def getSessionInfo: String = session match {
    case Some(s) if s.isOpen =>
      s"SSH Session Info: ${s.getUsername}@${s.getIoSession.getRemoteAddress}"
    case _ => "No active SSH session."
  }

  /**
   * Changes the current working directory on the remote server.
   *
   * @param path The path to the new working directory.
   * @return true if the directory change was successful, false otherwise.
   */
  def cd(path: String): Boolean = {
    val result = exec(s"cd $path && pwd")
    if (result.trim.nonEmpty) {
      currentDirectory = result.trim
      true
    } else {
      false
    }
  }

  /**
   * Retrieves the current working directory on the remote server.
   *
   * @return The path of the current working directory.
   */
  def pwd(): String = currentDirectory

  /**
   * Sends a local file to the remote server.
   *
   * @param localPath  The path of the local file to send.
   * @param remotePath The path where the file should be saved on the remote server.
   */
  def send(localPath: String, remotePath: String): Unit = withSftpClient { client =>
    val out = client.write(remotePath)
    try {
      Files.copy(Paths.get(localPath), out)
    } finally {
      out.close()
    }
  }

  /**
   * Sends a local file to the remote server.
   *
   * @param localFile  The local File object to send.
   * @param remotePath The path where the file should be saved on the remote server.
   */
  def send(localFile: File, remotePath: String): Unit = withSftpClient { client =>
    val out = client.write(remotePath)
    try {
      Files.copy(localFile.toPath, out)
    } finally {
      out.close()
    }
  }

  /**
   * Retrieves a file from the remote server.
   *
   * @param remotePath The path of the file on the remote server.
   * @param localPath  The path where the file should be saved locally.
   */
  def get(remotePath: String, localPath: String): Unit = withSftpClient { client =>
    val in = client.read(remotePath)
    try {
      Files.copy(in, Paths.get(localPath), StandardCopyOption.REPLACE_EXISTING)
    } finally {
      in.close()
    }
  }

  /**
   * Retrieves a file from the remote server using RemoteFile.
   *
   * @param remoteFile The RemoteFile object representing the file on the remote server.
   * @param localDirPath local path.
   *
   */
  def get(remoteFile: RemoteFile, localDirPath: String): Unit = withSftpClient { client =>
    val in = client.read(remoteFile.fullPath)
    try {
      Files.copy(in, Paths.get(localDirPath + remoteFile.name), StandardCopyOption.REPLACE_EXISTING)
    } finally {
      in.close()
    }
  }
  /**
   * Lists files and directories in the specified path on the remote server.
   *
   * @param path The path to list.
   * @return A List of RemoteFile objects representing the files and directories.
   */
  def listFiles(path: String): List[RemoteFile] = withSftpClient { client =>
    val homeDir = exec("echo $HOME").trim
    client.readDir(path).asScala.toList.map { entry =>
      val attrs = entry.getAttributes
      val isDirectory = attrs.isDirectory
      val size = attrs.getSize
      val fileName = entry.getFilename
      val fullPath = if (path.endsWith("/")) path + fileName else path + "/" + fileName
      val modifiedTime = Option(attrs.getModifyTime).map(_.toInstant)
      val depth = fullPath.count(_ == '/') - homeDir.count(_ == '/')
      RemoteFile(fileName, fullPath, isDirectory, size, modifiedTime, depth)
    }
  }

  /**
   * Executes a command on the remote server.
   *
   * @param command The command to execute.
   * @return The output of the command as a string.
   */
  def exec(command: String): String = {
    val channel = getSession.createExecChannel(command)
    try {
      val outputStream = new ByteArrayOutputStream()
      channel.setOut(outputStream)
      channel.open().verify(5000)
      channel.waitFor(util.EnumSet.of(ClientChannelEvent.CLOSED), 0L)
      outputStream.toString
    } finally {
      channel.close()
    }
  }

  /**
   * Enables or disables the keep-alive feature.
   *
   * When enabled, this feature prevents automatic session timeout by
   * sending periodic keep-alive messages.
   *
   * @param enable true to enable keep-alive, false to disable it.
   */
  def enableKeepAlive(enable: Boolean): Unit = {
    SSH.keepAliveEnabled = enable
    if (SSH.keepAliveEnabled && isConnected) {
      startKeepAlive()
    } else {
      stopKeepAlive()
    }
  }

  /**
   * Checks if the SSH session is currently connected.
   *
   * @return true if connected, false otherwise.
   */
  def isConnected: Boolean = session.exists(_.isOpen)

  private def getSession: ClientSession = session.getOrElse(throw new IllegalStateException("SSH session not connected."))

  private def withSftpClient[T](f: SftpClient => T): T = {
    val factory = SftpClientFactory.instance()
    Using.resource(factory.createSftpClient(getSession)) { client =>
      f(client)
    }
  }

  private def connectAndAuthenticate(): Unit = {
    val sess = client.connect(user, host, port).verify(5000).getSession
    sess.addPasswordIdentity(password)
    sess.auth().verify(5000)
    session = Some(sess)
  }

  private def startKeepAlive(): Unit = {
    executorService.scheduleAtFixedRate(() => {
      if (isConnected) {
        try {
          exec("echo 'keep-alive'")
          println("keep")
        } catch {
          case _: Exception => reconnect()
        }
      } else {
        reconnect()
      }
    }, 0, 60, TimeUnit.SECONDS)
  }

  private def stopKeepAlive(): Unit = {
    executorService.shutdown()
  }

  def reconnect(): Unit = {
    try {
      close()
      connectAndAuthenticate()
    } catch {
      case e: Exception => println(s"Failed to reconnect: ${e.getMessage}")
    }
  }
}

object SSH {
  var keepAliveEnabled: Boolean = false
}