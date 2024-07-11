// RSF.scala
package ssh

case class RSF(name: String, size: Long, isDir: Boolean, isHidden: Boolean, owner: String) {
  override def toString: String =
    s"$name (Size: $size bytes, Type: ${if (isDir) "Directory" else "File"}, ${if (isHidden) "Hidden" else "Visible"}, Owner: $owner)"
}