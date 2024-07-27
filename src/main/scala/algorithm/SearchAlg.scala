package algorithm

import ssh.{RemoteFile, SSH}
import global.Log

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

object SearchAlg {
  def searchAsync(ssh: SSH, searchTerm: String, showHidden: Boolean): Future[List[RemoteFile]] = {
    Future {
      search(ssh, searchTerm, showHidden)
    }
  }

  private def search(ssh: SSH, searchTerm: String, showHidden: Boolean): List[RemoteFile] = {
    if (searchTerm.isEmpty) {
      Log.err("Please enter a search term.")
      return List.empty
    }

    Try {
      val homeDir = ssh.exec("echo $HOME").trim
      val searchCommand = buildSearchCommand(homeDir, searchTerm, showHidden)
      val searchResults = ssh.exec(searchCommand).split("\n").filter(_.nonEmpty)

      searchResults.map { path =>
        val file = new java.io.File(path)
        val name = file.getName
        val isDirectory = file.isDirectory
        val depth = path.count(_ == '/') - homeDir.count(_ == '/')
        RemoteFile(name, path, isDirectory, -1, None, depth)
      }.toList
    }.getOrElse {
      Log.err(s"Search failed for term: $searchTerm")
      List.empty
    }
  }

  private def buildSearchCommand(homeDir: String, searchTerm: String, showHidden: Boolean): String = {
    if (showHidden) {
      s"find $homeDir -iname '*$searchTerm*'"
    } else {
      s"find $homeDir -not -path '*/.*' -iname '*$searchTerm*'"
    }
  }
}