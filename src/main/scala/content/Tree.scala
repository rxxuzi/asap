package content

import javafx.scene.control.cell.TextFieldTreeCell
import javafx.scene.control.{TreeCell, TreeItem, TreeView}
import javafx.util.{Callback, StringConverter}
import ssh.RemoteFile

import scala.jdk.CollectionConverters.*

object Tree {

  def createRemoteFileCellFactory(): Callback[TreeView[RemoteFile], TreeCell[RemoteFile]] = {
    _ =>
      new TextFieldTreeCell[RemoteFile](new StringConverter[RemoteFile]() {
        override def toString(rf: RemoteFile): String = rf.name

        override def fromString(string: String): RemoteFile = null
      })
  }

  def addPathToTree(root: TreeItem[RemoteFile], relativePath: String, fullPath: String, homeDir: String): Unit = {
    val parts = relativePath.split("/")
    var currentItem = root

    parts.zipWithIndex.foreach { case (part, index) =>
      val (isLast: Boolean, currentPath: String) = icp(homeDir, parts, index)
      val isDirectory = !isLast || fullPath.endsWith("/")

      currentItem.getChildren.asScala.find(_.getValue.name == part) match {
        case Some(existingItem) =>
          currentItem = existingItem
        case None =>
          val newItem = new TreeItem[RemoteFile](RemoteFile(part, currentPath, isDirectory))
          currentItem.getChildren.add(newItem)
          newItem.setExpanded(false)
          currentItem = newItem
      }
    }
  }

  private def icp(homeDir: String, parts: Array[String], index: Int) = {
    val isLast = index == parts.length - 1
    val currentPath = homeDir + "/" + parts.take(index + 1).mkString("/")
    (isLast, currentPath)
  }

  def createDirectoryOnlyTree(fileList: List[String], homeDir: String): TreeView[RemoteFile] = {
    val root = new TreeItem[RemoteFile](RemoteFile("~", homeDir, isDirectory = true))
    root.setExpanded(true)

    fileList.foreach { path =>
      val relativePath = path.replace(homeDir, "").stripPrefix("/")
      if (relativePath.nonEmpty) {
        addDirectoryPathToTree(root, relativePath, path, homeDir)
      }
    }

    val treeView = new TreeView[RemoteFile](root)
    treeView.setShowRoot(true)
    treeView.setCellFactory(_ => new javafx.scene.control.cell.TextFieldTreeCell[RemoteFile](new javafx.util.StringConverter[RemoteFile]() {
      override def toString(rf: RemoteFile): String = rf.name
      override def fromString(string: String): RemoteFile = null
    }))

    treeView
  }

  private def addDirectoryPathToTree(root: TreeItem[RemoteFile], relativePath: String, fullPath: String, homeDir: String): Unit = {
    val parts = relativePath.split("/")
    var currentItem = root

    parts.foreach { part =>
      val currentPath = currentItem.getValue.fullPath + "/" + part
      currentItem.getChildren.asScala.find(_.getValue.name == part) match {
        case Some(existingItem) =>
          currentItem = existingItem
        case None =>
          val newItem = new TreeItem[RemoteFile](RemoteFile(part, currentPath, isDirectory = true))
          currentItem.getChildren.add(newItem)
          currentItem = newItem
      }
    }
  }
}
