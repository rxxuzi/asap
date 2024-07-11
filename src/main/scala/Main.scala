import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage
import javafx.scene.layout.VBox
import javafx.scene.control.{TabPane, Tab, Label}
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
    
    val getTab = new Tab("Get")
    getTab.setClosable(false)
    getTab.setContent(new GetTab(sshManager, updateTitle _).getContent)

    val execTab = new Tab("Exec")
    execTab.setClosable(false)
    execTab.setContent(new ExecTab(sshManager, updateTitle _).getContent)
    
    val viewTab = new Tab("View")
    viewTab.setClosable(false)
    viewTab.setContent(new ViewTab(sshManager, updateTitle _).getContent)

    tabPane.getTabs.addAll(homeTab, sendTab, getTab, execTab, viewTab)

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

    stage.setOnCloseRequest(_ => sshManager.disconnect())
  }

  private def updateTitle(): Unit = {
    titleProperty.set(sshManager.getTitleInfo)
  }
}

object Main {
  def main(args: Array[String]): Unit = {
    Application.launch(classOf[Main], args: _*)
  }
}