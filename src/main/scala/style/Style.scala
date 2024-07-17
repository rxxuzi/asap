package style

import global.Config
import javafx.scene.Scene

object Style {
  val dark: (String, String) = ("#2C2C2C", "#E0E0E0") // bg fg
  val light: (String, String) = ("#FFFFFF", "#333333") // bg fg
  
  private val dir = "css"
  private var currentStyle: CSS = if (Config.dark) CSS.DARK else CSS.LIGHT

  private val styles = Map(
    CSS.MAIN -> "main.css",
    CSS.LIGHT -> "light.css",
    CSS.DARK -> "dark.css"
  )

  def getCurrentStyle: CSS = currentStyle

  def toggleStyle(scene: Scene): Unit = {
    currentStyle = if (currentStyle == CSS.LIGHT) CSS.DARK else CSS.LIGHT
    updateSceneStyle(scene)
  }
  
  def setStyleConfig(): Unit = {
    Config.setDark(currentStyle == CSS.DARK)
  }

  def updateSceneStyle(scene: Scene): Unit = {
    scene.getStylesheets.clear()
    scene.getStylesheets.add(set(CSS.MAIN))
    scene.getStylesheets.add(set(currentStyle))
  }

  def set(css: CSS): String = {
    if (Config.css) {
      styles.get(css) match {
        case Some(fileName) =>
          val resourcePath = s"/$dir/$fileName"
          val cssUrl = getClass.getResource(resourcePath)
          if (cssUrl == null) {
            throw new IllegalArgumentException(s"File not found: $resourcePath")
          }
          cssUrl.toExternalForm
        case None =>
          throw new IllegalArgumentException(s"No CSS file found for key: $css")
      }
    } else {
      ""
    }
  }
}