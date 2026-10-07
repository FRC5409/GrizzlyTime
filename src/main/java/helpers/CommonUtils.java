package helpers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Scanner;
import java.util.logging.Level;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

// methods in this class should not be dependent on anything relative
public class CommonUtils {
  /**
   * @author Dalton Smith CommonUtils Various utility methods used throughout the application
   *     https://code.makery.ch/blog/javafx-dialogs-official/
   */
  public static Application application;

  public static String getCurrentDir() {
    return System.getProperty("user.dir");
  }

  /**
   * Loads a bundled image, returning null when it is not there.
   *
   * <p>new Image(path) throws on a missing resource, which has taken the whole dialog down when
   * artwork was removed from the project but a constant still pointed at it. This artwork is
   * decorative, and a missing file must never stop someone logging in.
   */
  public static Image loadImage(String resource) {
    try (InputStream stream = CommonUtils.class.getClassLoader().getResourceAsStream(resource)) {
      if (stream == null) {
        LoggingUtils.log(Level.WARNING, "Missing image resource: " + resource);
        return null;
      }

      return new Image(stream);

    } catch (Exception e) {
      LoggingUtils.log(Level.WARNING, "Unable to load image: " + resource);
      return null;
    }
  }

  public static String readFile(String filePath) throws FileNotFoundException {
    File file = new File(filePath);
    Scanner sc = new Scanner(file);

    StringBuilder result = new StringBuilder();

    // read the entire json
    while (sc.hasNext()) {
      result.append(sc.next());
    }

    return result.toString();
  }

  public void playDing() {

    Media sound;
    try {
      sound = new Media(getClass().getResource("/sounds/ding.wav").toURI().toString());

    } catch (URISyntaxException e) {
      LoggingUtils.log(Level.SEVERE, e);
      return;
    }

    MediaPlayer mediaPlayer = new MediaPlayer(sound);
    mediaPlayer.play();
  }

  public static void exitApplication() {
    System.exit(1);
  }
}
