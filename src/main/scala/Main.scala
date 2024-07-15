import content.Status
import global.{Config, IO}
import javafx.application.Application
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Orientation
import javafx.scene.Scene
import javafx.scene.control.{SplitPane, Tab, TabPane, TextArea}
import javafx.scene.layout.{BorderPane, VBox}
import javafx.stage.Stage
import ssh.SSHManager

class Main extends Application {
  private val sshManager = new SSHManager()
  private val titleProperty = new SimpleStringProperty("ASAP")

  if (Config.ssh.autoConnect) {
    sshManager.connect(Config.ssh.configFile) match {
      case scala.util.Success(info) =>
        println(s"Connected: $info\n")
        updateTitle()
      case scala.util.Failure(ex) => println(s"Connection failed: ${ex.getMessage}\n")
    }
  }

  override def start(stage: Stage): Unit = {
    val tabPane = new TabPane()

    val homeTab = new Tab("Home")
    homeTab.setClosable(false)
    homeTab.setContent(new HomeTab(sshManager).getContent)

    val sendTab = new Tab("Send")
    sendTab.setClosable(false)
    sendTab.setContent(new SendTab(sshManager).getContent)

    val execTab = new Tab("Exec")
    execTab.setClosable(false)
    execTab.setContent(new ExecTab(sshManager).getContent)

    val viewTab = new Tab("View")
    viewTab.setClosable(false)
    viewTab.setContent(new ViewTab(sshManager).getContent)

    tabPane.getTabs.addAll(homeTab, sendTab, viewTab, execTab)

    val borderPane = new BorderPane()

    val topBox = new VBox(10,  tabPane)

    val statusArea = new TextArea()
    Status.initialize(statusArea)

    val splitPane = new SplitPane()
    splitPane.setOrientation(Orientation.VERTICAL)
    splitPane.getItems.addAll(topBox, statusArea)
    splitPane.setDividerPositions(0.8)

    borderPane.setCenter(splitPane)

    val scene = new Scene(borderPane, 800, 600)
    stage.setTitle("ASAP")
    stage.setScene(scene)
    stage.show()

    stage.setOnCloseRequest(_ => {
      sshManager.disconnect()
      exit()
    })
  }

  private def updateTitle(): Unit = {
    titleProperty.set(sshManager.getTitleInfo)
  }

  private def exit(): Unit = {
    Config.gen()
  }
}

object Main {
  private def init(): Unit = {
    Config.initialize()
    IO.mkdir(Config.dir.downloadsDir)
    if (Config.out.cache) IO.mkdir(Config.dir.cacheDir)
    if (Config.out.dbg) IO.mkdir(Config.dir.dbgDir)
  }

  def main(args: Array[String]): Unit = {
    init()
    Application.launch(classOf[Main], args: _*)
  }
}