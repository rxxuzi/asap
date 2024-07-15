package ssh

import java.io.File

trait SSHTrait {
  def open(): Unit
  def close(): Unit
  def send(localPath: String, remotePath: String): Unit
  def send(localFile: File, remotePath: String): Unit
  def get(remotePath: String, localPath: String): Unit
  def exec(command: String): String
}