package ssh

import java.time.Instant

case class RemoteFile(
                       name: String,
                       fullPath: String,
                       isDirectory: Boolean,
                       size: Long = -1,
                       modifiedTime: Option[Instant] = None,
                       depth: Int = 0
                     ) {
  override def toString: String =
    s"""
       |${if (isDirectory) "[d]" else "[f]"} $fullPath
       |""".stripMargin
}