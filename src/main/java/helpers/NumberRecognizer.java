package helpers;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;

/**
 * NumberRecognizer Reads the ID number printed on a card out of a captured camera frame.
 *
 * <p>Only the band inside the on screen guide box is read, and a result has to be a whole line of
 * nothing but digits, so the names, logos and dates elsewhere on the card cannot be mistaken for an
 * ID.
 */
public class NumberRecognizer {

  // a candidate line is digits and nothing else, with no gap anywhere in it
  private static final Pattern kDigitLine = Pattern.compile("\\d+");

  private ITesseract tesseract;
  private boolean unavailable = false;

  /** The outcome of a capture: the digits found, and whether they look like a usable ID. */
  public static class Result {
    public final String digits;
    public final boolean validId;

    Result(String digits, boolean validId) {
      this.digits = digits;
      this.validId = validId;
    }
  }

  /** Whether the native text recognition engine could not be loaded at all. */
  public boolean isUnavailable() {
    return unavailable;
  }

  /**
   * Reads the guide area of a frame and returns the best ID shaped number in it, or null when
   * nothing readable was found.
   */
  public Result read(BufferedImage frame) {
    if (frame == null) {
      return null;
    }

    ITesseract engine = engine();

    if (engine == null) {
      return null;
    }

    try {
      String text = engine.doOCR(prepare(guideRegion(frame)));
      return bestCandidate(text);

    } catch (Exception e) {
      LoggingUtils.log(Level.WARNING, "Unable to read the captured frame");
      LoggingUtils.log(Level.SEVERE, e);
      return null;
    }
  }

  /**
   * Picks the number most likely to be the ID.
   *
   * <p>A candidate has to be a whole line of nothing but digits, with no gap anywhere in it, so two
   * separated groups on one line are never joined and neither are digits on different lines. What
   * comes back is a single number exactly as it is printed.
   *
   * <p>A run of an accepted length wins. Otherwise the longest is returned and flagged, so a near
   * miss can be corrected by hand instead of being silently dropped.
   */
  private Result bestCandidate(String text) {
    List<String> candidates = new ArrayList<>();

    for (String line : text.split("\\R")) {
      String trimmed = line.trim();

      if (!trimmed.isEmpty() && kDigitLine.matcher(trimmed).matches()) {
        candidates.add(trimmed);
      }
    }

    LoggingUtils.log(Level.INFO, "Capture candidates: " + candidates);

    if (candidates.isEmpty()) {
      return null;
    }

    for (String candidate : candidates) {
      if (CommonUtils.isAcceptedIdLength(candidate.length())) {
        return new Result(candidate, true);
      }
    }

    String longest = candidates.get(0);

    for (String candidate : candidates) {
      if (candidate.length() > longest.length()) {
        longest = candidate;
      }
    }

    return new Result(longest, false);
  }

  /** Crops to the area the guide box marks out. */
  private BufferedImage guideRegion(BufferedImage frame) {
    int width = (int) (frame.getWidth() * Constants.kCameraGuideWidthFraction);
    int height = (int) (frame.getHeight() * Constants.kCameraGuideHeightFraction);

    if (width <= 0 || height <= 0) {
      return frame;
    }

    try {
      return frame.getSubimage(
          (frame.getWidth() - width) / 2, (frame.getHeight() - height) / 2, width, height);

    } catch (RasterFormatException e) {
      return frame;
    }
  }

  /** Greyscales and scales the crop up, which is what Tesseract reads most reliably. */
  private BufferedImage prepare(BufferedImage region) {
    int scale = region.getHeight() < Constants.kCaptureMinHeight ? 2 : 1;
    int width = region.getWidth() * scale;
    int height = region.getHeight() * scale;

    BufferedImage grey = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
    Graphics2D graphics = grey.createGraphics();
    graphics.setRenderingHint(
        RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    graphics.drawImage(region, 0, 0, width, height, null);
    graphics.dispose();

    return grey;
  }

  /** Builds the engine on first use, and gives up quietly if the native library is missing. */
  private synchronized ITesseract engine() {
    if (tesseract != null || unavailable) {
      return tesseract;
    }

    try {
      Tesseract engine = new Tesseract();
      engine.setDatapath(tessdata());
      engine.setLanguage(Constants.kCaptureLanguage);

      // digits only - the ID is numeric, and this keeps letters elsewhere on the
      // card from being read as part of the number
      engine.setVariable("tessedit_char_whitelist", "0123456789");

      // treat the guide area as a block of text rather than forcing one line,
      // so a card with the number on its own line still reads correctly
      engine.setPageSegMode(Constants.kCapturePageSegMode);

      tesseract = engine;
      return tesseract;

    } catch (Throwable e) {
      LoggingUtils.log(Level.SEVERE, "Text recognition unavailable");
      LoggingUtils.log(Level.SEVERE, e);
      unavailable = true;
      return null;
    }
  }

  /**
   * Finds the language data. A tessdata folder beside the application wins, matching how the images
   * and stylesheet can already be overridden; otherwise the copy inside the jar is unpacked.
   */
  private String tessdata() throws Exception {
    File local = new File(CommonUtils.getCurrentDir() + File.separator + Constants.kTessdataFolder);

    if (new File(local, Constants.kCaptureLanguage + ".traineddata").exists()) {
      LoggingUtils.log(Level.INFO, "Using tessdata from " + local);
      return local.getAbsolutePath();
    }

    Path unpacked = Files.createTempDirectory("grizzlytime-tessdata");
    unpacked.toFile().deleteOnExit();

    String name = Constants.kCaptureLanguage + ".traineddata";

    try (InputStream in =
        getClass().getClassLoader().getResourceAsStream(Constants.kTessdataFolder + "/" + name)) {

      if (in == null) {
        throw new IllegalStateException("No bundled " + name);
      }

      Path target = unpacked.resolve(name);
      Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
      target.toFile().deleteOnExit();
    }

    LoggingUtils.log(Level.INFO, "Unpacked tessdata to " + unpacked);
    return unpacked.toString();
  }
}
