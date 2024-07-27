package command

object BuildCommand {
  private def baseCmd(hidden: Boolean) =
    s"find $$HOME ${if (!hidden) "-not -path '*/.*'" else ""}"

  private def generateSearchPattern(query: String): String = {
    query.split("\\s+").filter(_.nonEmpty).map { term =>
      if (term.endsWith("/")) s"-type d -name '*${term.dropRight(1)}*'"
      else if (term.startsWith("*.")) s"-type f -name '$term'"
      else s"-path '*$term*'"
    }.mkString(" -and ")
  }

  def search(hidden: Boolean, alg: SortBy, query: String): String = {
    val searchPattern = generateSearchPattern(query)
    s"${baseCmd(hidden)} $searchPattern | ${alg.cmd}"
  }

  def sort(hidden: Boolean, alg: SortBy): String = {
    val base = baseCmd(hidden)
    val sortCmd = alg.cmd
    val typeOptions = List("d", "f").map(t => s"$base -type $t | $sortCmd")
    typeOptions.mkString("; echo; ")
  }
}
