// SSHJson.scala
package ssh

import java.io.File
import com.google.gson.{Gson, GsonBuilder}

case class SSHJson(host: String, port: Int, user: String, pass: String) {
  override def toString: String = s"$user@$host:$port:$pass"
}

object SSHJson {
  def apply(file: File): SSHJson = {
    val gson = new GsonBuilder().setPrettyPrinting().create()
    val reader = scala.io.Source.fromFile(file)
    try {
      gson.fromJson(reader.mkString, classOf[SSHJson])
    } finally {
      reader.close()
    }
  }

  def apply(path: String): SSHJson = apply(new File(path))
//  implicit class SSHJsonOps(sshJson: SSHJson) {
//  }
}

