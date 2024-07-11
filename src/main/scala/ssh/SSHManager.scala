package ssh

import scala.util.Try

class SSHManager {
  private var sshOpt: Option[SSH] = None
  private var homeDirectory: String = "/"
  private var currentPath: String = "/"

  def connect(configPath: String): Try[String] = Try {
    val ssh = new SSH(configPath)
    ssh.open()
    sshOpt = Some(ssh)
    homeDirectory = ssh.exec("echo $HOME").trim
    currentPath = homeDirectory
    ssh.getSessionInfo
  }

  def disconnect(): Unit = {
    sshOpt.foreach(_.close())
    sshOpt = None
    homeDirectory = "/"
    currentPath = "/"
  }

  def withSSH[T](f: SSH => T): Try[T] = {
    sshOpt match {
      case Some(ssh) => Try(f(ssh))
      case None => Try(throw new IllegalStateException("SSH not connected"))
    }
  }

  def getUser: String = sshOpt.map(_.user).getOrElse("")
  def getHost: String = sshOpt.map(_.host).getOrElse("")
  def getPort: Int = sshOpt.map(_.port).getOrElse(0)
  def getPass: String = sshOpt.map(_.password).getOrElse("")
  def getCurrentPath: String = currentPath
  def getHomeDirectory: String = homeDirectory

  def isConnected: Boolean = sshOpt.exists(_.getSessionInfo.startsWith("SSH Session Info:"))

  def updatePath(newPath: String): Unit = {
    currentPath = newPath
  }

  def getTitleInfo: String = {
    if (isConnected) {
      s"$getUser@$getHost:$getCurrentPath"
    } else {
      "Jingliu"
    }
  }
}