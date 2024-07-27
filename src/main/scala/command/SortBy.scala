package command

enum SortBy(val displayName: String) {
  case NameAsc extends SortBy("Name (A-Z)")
  case NameDesc extends SortBy("Name (Z-A)")
  case SizeAsc extends SortBy("Size (Ascending)")
  case SizeDesc extends SortBy("Size (Descending)")
  case DateAsc extends SortBy("Date Modified (Oldest First)")
  case DateDesc extends SortBy("Date Modified (Newest First)")

  def cmd: String = this match {
    case NameAsc    => "sort"
    case NameDesc   => "sort -r"
    case SizeAsc    => "xargs stat -c '%s %n' | sort -n | awk '{print $2}'"
    case SizeDesc   => "xargs stat -c '%s %n' | sort -nr | awk '{print $2}'"
    case DateAsc    => "xargs stat -c '%Y %n' | sort -n | awk '{print $2}'"
    case DateDesc   => "xargs stat -c '%Y %n' | sort -nr | awk '{print $2}'"
  }
}
