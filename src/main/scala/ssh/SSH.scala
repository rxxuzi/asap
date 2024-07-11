package ssh

import com.jcraft.jsch._
import java.io._
import scala.jdk.CollectionConverters._
import scala.util.Using

class SSH(val host: String, val port: Int, val user: String, val password: String) extends AutoCloseable {
  val userHost: String = s"$user@$host"
  private var session: Option[Session] = None

  def this(path: String) = {
    this(SSHJson(path).host, SSHJson(path).port, SSHJson(path).user, SSHJson(path).pass)
  }

  def open(): Unit = {
    val jsch = new JSch()
    val sess = jsch.getSession(user, host, port)
    sess.setConfig("StrictHostKeyChecking", "no")
    sess.setPassword(password)
    sess.connect()
    session = Some(sess)
  }

  override def close(): Unit = {
    session.foreach(_.disconnect())
    session = None
  }

  def getSessionInfo: String = session match {
    case Some(s) if s.isConnected =>
      s"SSH Session Info: ${s.getUserName}@${s.getHost}:${s.getPort}"
    case _ => "No active SSH session."
  }

  def send(localPath: String, remotePath: String): Unit = withSftpChannel { channel =>
    channel.put(localPath, remotePath)
  }

  def send(localFile: File, remotePath: String): Unit = withSftpChannel { channel =>
    Using(new FileInputStream(localFile)) { fis =>
      channel.put(fis, remotePath)
    }.get
  }

  def get(remotePath: String, localPath: String): Unit = withSftpChannel { channel =>
    channel.get(remotePath, localPath)
  }

  def exec(command: String): String = {
    val channel = getSession.openChannel("exec").asInstanceOf[ChannelExec]
    try {
      channel.setCommand(command)
      channel.setInputStream(null)
      channel.setErrStream(System.err)
      Using(channel.getInputStream) { in =>
        channel.connect()
        new String(in.readAllBytes())
      }.get
    } finally {
      if (channel.isConnected) channel.disconnect()
    }
  }

  def output(channel: Channel): String = {
    Using.resource(new BufferedReader(new InputStreamReader(channel.getInputStream))) { reader =>
      channel.connect()
      Iterator.continually(reader.readLine()).takeWhile(_ != null).mkString("\n")
    }
  }

  def listFiles(path: String): List[RSF] = withSftpChannel { channel =>
    channel.ls(path).asScala.toList.map { entry =>
      val lsEntry = entry.asInstanceOf[ChannelSftp#LsEntry]
      val isDirectory = lsEntry.getAttrs.isDir
      val size = if (isDirectory) -1L else lsEntry.getAttrs.getSize
      val fileName = lsEntry.getFilename
      val isHidden = fileName.startsWith(".")
      val uid = lsEntry.getAttrs.getUId
      val owner = uid.toString
      RSF(fileName, size, isDirectory, isHidden, owner)
    }
  }

  private def getSession: Session = session.getOrElse(throw new IllegalStateException("SSH session not connected."))

  private def withSftpChannel[T](f: ChannelSftp => T): T = {
    val channel = getSession.openChannel("sftp").asInstanceOf[ChannelSftp]
    try {
      channel.connect()
      f(channel)
    } finally {
      if (channel.isConnected) {
        channel.exit()
        channel.disconnect()
      }
    }
  }
}