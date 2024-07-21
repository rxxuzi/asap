package content

import javafx.geometry.{Insets, Pos}
import javafx.scene.control.{Button, Label, Slider}
import javafx.scene.layout.{BorderPane, HBox, StackPane}
import javafx.scene.media.{Media, MediaPlayer, MediaView}
import javafx.util.Duration
import style.{CSS, Style}

import java.io.File

/**
 * A custom video player component built with JavaFX.
 *
 * This class provides a functional video player with play/pause,
 * seek, and volume control capabilities.
 *
 * @param file The video file to be played
 */
class VideoPlayer(file: File) extends BorderPane {

  private val media = new Media(file.toURI.toString)
  private val mediaPlayer = new MediaPlayer(media)
  private val mediaView = new MediaView(mediaPlayer)
  private val playPauseButton = new Button("▶")
  private val timeLabel = new Label("00:00 / 00:00")
  private val seekBar = new Slider()
  private val volumeSlider = new Slider(0, 1, 0.5)

  // Initialize the UI and set up event handlers
  setupUI()
  setupMediaPlayer()
  applyStyles()

  /**
   * Sets up the user interface components and layouts.
   */
  private def setupUI(): Unit = {
    mediaView.fitWidthProperty().bind(this.widthProperty())
    mediaView.fitHeightProperty().bind(this.heightProperty())

    val controls = new HBox(10, playPauseButton, timeLabel, seekBar, volumeSlider)
    controls.setAlignment(Pos.CENTER)
    controls.setPadding(new Insets(10))

    this.setCenter(new StackPane(mediaView))
    this.setBottom(controls)

    setupControlActions()
  }

  /**
   * Sets up the media player and its event handlers.
   */
  private def setupMediaPlayer(): Unit = {
    mediaPlayer.currentTimeProperty().addListener((_, _, newValue) => {
      updateTimeLabel()
      if (!seekBar.isValueChanging) {
        seekBar.setValue(newValue.toSeconds / mediaPlayer.getTotalDuration.toSeconds * 100)
      }
    })

    mediaPlayer.setOnReady(() => {
      seekBar.setMax(100)
      updateTimeLabel()
    })

    mediaPlayer.setOnEndOfMedia(() => {
      playPauseButton.setText("▶")
      mediaPlayer.seek(Duration.ZERO)
      mediaPlayer.pause()
    })
  }

  /**
   * Sets up action handlers for control buttons and sliders.
   */
  private def setupControlActions(): Unit = {
    playPauseButton.setOnAction(_ => togglePlayPause())

    seekBar.valueProperty().addListener((_, _, newValue) => {
      if (seekBar.isValueChanging) {
        mediaPlayer.seek(mediaPlayer.getTotalDuration.multiply(newValue.doubleValue() / 100.0))
      }
    })

    volumeSlider.valueProperty().addListener((_, _, newValue) => {
      mediaPlayer.setVolume(newValue.doubleValue())
    })
  }

  /**
   * Toggles between play and pause states.
   * If the video has ended, it will restart from the beginning when play is pressed.
   */
  private def togglePlayPause(): Unit = {
    if (mediaPlayer.getStatus == MediaPlayer.Status.PLAYING) {
      mediaPlayer.pause()
      playPauseButton.setText("▶")
    } else {
      if (mediaPlayer.getCurrentTime == mediaPlayer.getTotalDuration) {
        mediaPlayer.seek(Duration.ZERO)
      }
      mediaPlayer.play()
      playPauseButton.setText("⏸")
    }
  }

  /**
   * Updates the time label with current and total playback time.
   */
  private def updateTimeLabel(): Unit = {
    val current = mediaPlayer.getCurrentTime
    val total = mediaPlayer.getTotalDuration
    timeLabel.setText(s"${formatTime(current)} / ${formatTime(total)}")
  }

  /**
   * Formats a Duration object into a string representation (MM:SS).
   *
   * @param duration The Duration to format
   * @return A string representation of the duration in MM:SS format
   */
  private def formatTime(duration: Duration): String = {
    val seconds = duration.toSeconds.toInt
    f"${seconds / 60}%02d:${seconds % 60}%02d"
  }

  /**
   * Applies CSS styles to the video player and its components.
   */
  private def applyStyles(): Unit = {
    // Apply the main CSS
    this.getStylesheets.add(Style.set(CSS.MAIN))

    // Apply the current style (LIGHT or DARK)
    this.getStylesheets.add(Style.set(Style.getCurrentStyle))

    // Apply the player-specific CSS
    this.getStylesheets.add(Style.set(CSS.PLAYER))

    // Add custom class for additional styling if needed
    this.getStyleClass.add("content-viewer")

    // Set styles for specific components
    playPauseButton.getStyleClass.add("control-button")
    seekBar.getStyleClass.add("seek-bar")
    volumeSlider.getStyleClass.add("volume-slider")
    timeLabel.getStyleClass.add("time-label")
  }
}