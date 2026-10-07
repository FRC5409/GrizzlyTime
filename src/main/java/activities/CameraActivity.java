package activities;

import com.github.sarxos.webcam.Webcam;
import helpers.Constants;
import helpers.LoggingUtils;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;

/**
 * CameraActivity Shows a live preview of the computer's camera and hands over the current frame
 * when the user asks to capture one. Nothing is read from the camera until then.
 *
 * <p>Every failure path here is non-fatal on purpose: if the camera is missing or dies, the USB
 * scanner and the keyboard must keep working.
 */
public class CameraActivity {

  private final ImageView cameraFrame = new ImageView();

  private Webcam webcam;
  private Thread grabberThread;

  // the most recent full size frame, replaced by the grabber and read by capture
  private volatile BufferedImage latestFrame;

  // control flow, touched from both the FX thread and the grabber thread
  private volatile boolean stopCamera = false;
  private volatile boolean running = false;

  private Consumer<String> onStatus = status -> {};

  public CameraActivity() {
    // the preview is mirrored so holding a card up behaves like a mirror.
    // the frame handed to capture is left as the camera saw it.
    cameraFrame.setScaleX(-1);
    cameraFrame.setPreserveRatio(true);
    cameraFrame.setFitWidth(Constants.kCameraFeedWidth);
    cameraFrame.setFitHeight(Constants.kCameraFeedHeight);

    // registered once, not per start, so repeated toggling cannot pile up hooks
    Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "camera-shutdown"));
  }

  public ImageView getCameraFrame() {
    return cameraFrame;
  }

  /** Called on the FX thread whenever the camera's status line should change. */
  public void setOnStatus(Consumer<String> onStatus) {
    this.onStatus = onStatus;
  }

  /**
   * The frame currently on screen, at full capture resolution, or null when the camera is not
   * running yet. This is what the ID number is read from.
   */
  public BufferedImage captureFrame() {
    return latestFrame;
  }

  /**
   * Opens the camera and starts the preview. Returns immediately - discovery and opening can take a
   * second or more, so they happen off the FX thread.
   */
  public synchronized void start() {
    if (running) {
      return;
    }

    running = true;
    stopCamera = false;

    publishStatus(Constants.kCameraStatusStarting);

    Thread starter = new Thread(this::openAndStream, "camera-start");
    starter.setDaemon(true);
    starter.start();
  }

  public boolean isRunning() {
    return running;
  }

  private void openAndStream() {
    try {
      List<Webcam> available = Webcam.getWebcams();

      if (available.isEmpty()) {
        LoggingUtils.log(Level.WARNING, "No camera detected");
        running = false;
        publishStatus(Constants.kCameraStatusUnavailable);
        return;
      }

      webcam = available.get(0);
      LoggingUtils.log(Level.INFO, "Using camera: " + webcam.getName());

      applyViewSize(webcam);

      if (!webcam.open()) {
        LoggingUtils.log(Level.WARNING, "Camera found but could not be opened");
        running = false;
        publishStatus(Constants.kCameraStatusUnavailable);
        return;
      }

      // the user may have switched the toggle back off while the device opened
      if (stopCamera) {
        closeWebcam();
        return;
      }

      publishStatus(Constants.kCameraStatusScanning);

      grabberThread = new Thread(this::grabFrames, "camera-grabber");
      grabberThread.setDaemon(true);
      grabberThread.start();

    } catch (Throwable e) {
      // Throwable, not Exception: a missing or incompatible native library surfaces
      // as an Error, and that still must not be fatal to the application.
      LoggingUtils.log(Level.SEVERE, "Unable to start camera");
      LoggingUtils.log(Level.SEVERE, e);
      running = false;
      publishStatus(Constants.kCameraStatusUnavailable);
    }
  }

  /**
   * Picks the capture resolution, preferring the largest.
   *
   * <p>The driver under-reports what the hardware can do - on Linux it commonly advertises nothing
   * above VGA for a camera that handles 1080p - so the preferred sizes are registered as custom
   * sizes rather than being chosen from the advertised list. Those extra pixels are what make the
   * number printed on a card legible.
   */
  private void applyViewSize(Webcam webcam) {
    List<Dimension> candidates = new ArrayList<>();

    for (int[] size : Constants.kCameraCaptureSizes) {
      candidates.add(new Dimension(size[0], size[1]));
    }

    try {
      List<Dimension> custom = new ArrayList<>(candidates);
      custom.addAll(Arrays.asList(webcam.getViewSizes()));
      webcam.setCustomViewSizes(custom.toArray(new Dimension[0]));

    } catch (Exception e) {
      LoggingUtils.log(Level.INFO, "Camera rejected custom view sizes, using advertised ones");
    }

    for (Dimension wanted : candidates) {
      try {
        webcam.setViewSize(wanted);
        LoggingUtils.log(Level.INFO, "Camera capture size " + wanted.width + "x" + wanted.height);
        return;

      } catch (Exception e) {
        LoggingUtils.log(Level.INFO, "Camera rejected " + wanted.width + "x" + wanted.height);
      }
    }

    LoggingUtils.log(Level.INFO, "Using the camera's default capture size");
  }

  private void grabFrames() {
    while (!stopCamera) {
      try {
        Webcam current = webcam;

        if (current == null || !current.isOpen()) {
          return;
        }

        BufferedImage frame = current.getImage();

        if (frame == null) {
          Thread.sleep(Constants.kCameraScanIntervalMs);
          continue;
        }

        latestFrame = frame;

        // the preview is a fraction of the capture size, so scale down here
        // rather than handing JavaFX a full size image every frame
        BufferedImage preview = scaleForPreview(frame);
        runOnFxThread(() -> cameraFrame.setImage(SwingFXUtils.toFXImage(preview, null)));

        Thread.sleep(Constants.kCameraScanIntervalMs);

      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;

      } catch (Exception e) {
        // a close racing the grab is expected when the toggle goes off
        if (stopCamera) {
          return;
        }

        LoggingUtils.log(Level.SEVERE, e);
        running = false;
        publishStatus(Constants.kCameraStatusUnavailable);
        stopCamera = true;
      }
    }
  }

  /** Shrinks a capture frame to preview size, keeping its aspect ratio. */
  private BufferedImage scaleForPreview(BufferedImage frame) {
    int width = Constants.kCameraFeedWidth;
    int height = Math.max(1, frame.getHeight() * width / frame.getWidth());

    BufferedImage preview = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = preview.createGraphics();
    graphics.setRenderingHint(
        RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    graphics.drawImage(frame, 0, 0, width, height, null);
    graphics.dispose();

    return preview;
  }

  /**
   * Closes the camera and clears the preview. Safe to call more than once, from any thread, and
   * before start() - the camera can be started again afterwards.
   */
  public synchronized void stop() {
    stopCamera = true;
    running = false;

    if (grabberThread != null) {
      grabberThread.interrupt();
      grabberThread = null;
    }

    closeWebcam();
    latestFrame = null;

    runOnFxThread(() -> cameraFrame.setImage(null));
  }

  private void closeWebcam() {
    try {
      if (webcam != null && webcam.isOpen()) {
        webcam.close();
      }
    } catch (Exception e) {
      LoggingUtils.log(Level.WARNING, "Error closing camera");

    } finally {
      webcam = null;
    }
  }

  private void publishStatus(String status) {
    runOnFxThread(() -> onStatus.accept(status));
  }

  /**
   * Posts to the FX thread, tolerating the toolkit being gone. stop() runs from a JVM shutdown
   * hook, by which point JavaFX has usually shut down and Platform.runLater throws.
   */
  private void runOnFxThread(Runnable action) {
    try {
      Platform.runLater(action);

    } catch (IllegalStateException e) {
      // toolkit already shut down, nothing left to update
    }
  }
}
