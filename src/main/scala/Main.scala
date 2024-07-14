import global.{Config, IO}
import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage
import javafx.scene.layout.VBox
import javafx.scene.control.{Label, Tab, TabPane}
import javafx.geometry.Insets
import ssh.SSHManager
import javafx.beans.property.SimpleStringProperty

class Main extends Application {
  private val sshManager = new SSHManager()
  private val titleProperty = new SimpleStringProperty("ASAP")
  

  override def start(stage: Stage): Unit = {
    val tabPane = new TabPane()

    val homeTab = new Tab("Home")
    homeTab.setClosable(false)
    homeTab.setContent(new HomeTab(sshManager, updateTitle _).getContent)

    val sendTab = new Tab("Send")
    sendTab.setClosable(false)
    sendTab.setContent(new SendTab(sshManager, updateTitle _).getContent)


    val execTab = new Tab("Exec")
    execTab.setClosable(false)
    execTab.setContent(new ExecTab(sshManager, updateTitle _).getContent)
    
    val viewTab = new Tab("View")
    viewTab.setClosable(false)
    viewTab.setContent(new ViewTab(sshManager, updateTitle _).getContent)

    tabPane.getTabs.addAll(homeTab, sendTab, viewTab, execTab)

    val root = new VBox(10)
    root.setPadding(new Insets(10))

    val titleLabel = new Label()
    titleLabel.textProperty().bind(titleProperty)
    titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;")

    root.getChildren.addAll(titleLabel, tabPane)

    val scene = new Scene(root, 800, 600)
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