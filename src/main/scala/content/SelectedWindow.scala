package content

import global.Config.getClass
import global.{Config, Log}
import javafx.collections.{FXCollections, ObservableList}
import javafx.geometry.{Insets, Orientation, Pos}
import javafx.scene.Scene
import javafx.scene.control.{Button, ListView, SelectionMode, SplitPane}
import javafx.scene.image.Image
import javafx.scene.layout.{HBox, Priority, VBox}
import javafx.stage.Stage
import style.Style

import java.io.File
import scala.jdk.CollectionConverters.*

final class SelectedWindow(files: ObservableList[File], onUpdate: ObservableList[File] => Unit) extends Stage {
  private val listView = new ListView[File](files)
  listView.getSelectionModel.setSelectionMode(SelectionMode.MULTIPLE)

  private val removeButton = new Button("Remove")
  private val clearButton = new Button("Clear All")
  private val preview = new javafx.scene.control.ScrollPane()
  preview.setFitToWidth(true)
  preview.setFitToHeight(true)

  private val scene = new Scene(new VBox(), 1000, 600)
  this.setScene(scene)
  this.setTitle("Selected Files")
  try {
    val iconStream = getClass.getResourceAsStream(Config.iconPath)
    if (iconStream != null) {
      this.getIcons.add(new Image(iconStream))
    } else {
      Log.warn(s"Icon file not found: ${Config.iconPath}")
    }
  } catch {
    case ex: Exception => Log.err(s"Failed to load icon: ${ex.getMessage}")
  }
  Style.updateSceneStyle(scene) // set style

  private var viewer = new LocalFileContentViewer(scene.getWidth * 0.7 * 0.9, scene.getHeight * 0.9)


  removeButton.setOnAction(_ => {
    val selected: ObservableList[File] = listView.getSelectionModel.getSelectedItems
    if (!selected.isEmpty) {
      // Create a new list with items that are not selected for removal
      val remainingFiles = files.asScala.filter(file => !selected.contains(file))
      // Clear the original list and add all remaining files
      files.clear()
      files.addAll(FXCollections.observableArrayList(remainingFiles.asJava))
      updatePreview()
      // Call onUpdate with a new ObservableList containing the remaining files
      onUpdate(FXCollections.observableArrayList(remainingFiles.asJava))
    }
  })

  clearButton.setOnAction(_ => {
    files.clear()
    onUpdate(files)
    preview.setContent(null)
  })

  private val buttonBox = new HBox(10, removeButton, clearButton)
  buttonBox.setAlignment(Pos.CENTER)

  listView.getSelectionModel.selectedItemProperty().addListener((_, _, newValue) => {
    if (newValue != null) {
      val contentNode = viewer.viewContent(newValue)
      preview.setContent(contentNode)
    } else {
      preview.setContent(null)
    }
  })

  private val leftPane = new VBox(10, listView, buttonBox)
  leftPane.setPadding(new Insets(10))
  VBox.setVgrow(listView, Priority.ALWAYS)

  private val splitPane = new SplitPane()
  splitPane.setOrientation(Orientation.HORIZONTAL)
  splitPane.getItems.addAll(leftPane, preview)
  splitPane.setDividerPositions(0.3)

  scene.setRoot(splitPane)

  scene.widthProperty().addListener((_, _, newValue) => {
    viewer = new LocalFileContentViewer(newValue.doubleValue() * 0.7 * 0.9, scene.getHeight * 0.9)
    updatePreview()
  })

  scene.heightProperty().addListener((_, _, newValue) => {
    viewer = new LocalFileContentViewer(scene.getWidth * 0.7 * 0.9, newValue.doubleValue() * 0.9)
    updatePreview()
  })

  listView.setCellFactory(_ => new javafx.scene.control.ListCell[File] {
    override def updateItem(item: File, empty: Boolean): Unit = {
      super.updateItem(item, empty)
      if (empty || item == null) setText(null)
      else setText(item.getName)
    }
  })

  private def updatePreview(): Unit = {
    val selectedFile = listView.getSelectionModel.getSelectedItem
    if (selectedFile != null) {
      val contentNode = viewer.viewContent(selectedFile)
      preview.setContent(contentNode)
    } else {
      preview.setContent(null)
    }
  }
}