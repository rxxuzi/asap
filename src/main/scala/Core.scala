import content.Status
import global.Config
import javafx.application.Application
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Orientation
import javafx.scene.Scene
import javafx.scene.control.{SplitPane, Tab, TabPane, TextArea}
import javafx.scene.image.Image
import javafx.scene.layout.{BorderPane, VBox}
import javafx.stage.Stage
import ssh.SSHManager
import style.Style

import scala.concurrent.ExecutionContext.Implicits.global
import _root_.global.Config.getClass
import util.Downloads

final class Core extends Application {
  private val sshManager = new SSHManager()
  private val titleProperty = new SimpleStringProperty("ASAP")
  private var stage: Stage = _

  Downloads.setSSHManager(sshManager)

  if (Config.ssh.autoConnect) {
    sshManager.connect(Config.ssh.configFile) match {
      case scala.util.Success(info) =>
        println(s"Connected: $info\n")
      case scala.util.Failure(ex) => println(s"Connection failed: ${ex.getMessage}\n")
    }
  }

  override def start(primaryStage: Stage): Unit = {
    stage = primaryStage
    val scene = new Scene(new BorderPane(), 900, 600)
    Style.updateSceneStyle(scene)

    val tabPane = createTabPane(scene)

    val borderPane = new BorderPane()

    val topBox = new VBox(10, tabPane)

    val statusArea = new TextArea()
    Status.initialize(statusArea)

    val splitPane = new SplitPane()
    splitPane.setOrientation(Orientation.VERTICAL)
    splitPane.getItems.addAll(topBox, statusArea)
    splitPane.setDividerPositions(0.8)

    borderPane.setCenter(splitPane)

    scene.setRoot(borderPane)

    stage.setScene(scene)
    show()
  }

  private def show(): Unit = {
    stage.setTitle("ASAP")
    stage.setOnCloseRequest(_ => {
      sshManager.disconnect()
      exit()
    })

    stage.getIcons.add(new Image(getClass.getResourceAsStream(Config.iconPath)))
    stage.show()
  }

  private def createTabPane(scene: Scene): TabPane = {
    val tabPane = new TabPane()

    val homeTab = createTab("Home", new HomeTab(sshManager, scene).getContent)
    val sendTab = createTab("Send", new SendTab(sshManager).getContent)
    val execTab = createTab("Exec", new ExecTab(sshManager).getContent)
    val viewTab = createTab("View", new ViewTab(sshManager).getContent)
    val downloadTab = createTab("Download", new DownloadTab().getContent)

    tabPane.getTabs.addAll(homeTab, sendTab, viewTab, downloadTab, execTab)
    tabPane
  }

  private def createTab(name: String, content: javafx.scene.Node): Tab = {
    val tab = new Tab(name)
    tab.setClosable(false)
    tab.setContent(content)
    tab
  }

  private def exit(): Unit = {
    Style.setStyleConfig()
    Config.write()
  }
}
