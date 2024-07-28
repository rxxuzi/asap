package ssh

import java.io.File

trait SSHTrait {
  def open(): Unit
  def close(): Unit
  def send(localPath: String, remotePath: String): Boolean
  def send(localFile: File, remotePath: String): Boolean
  def get(remotePath: String, localPath: String): Boolean
  def get(remoteFile: RemoteFile, localDirPath: String): Boolean
  def exec(command: String): String
}