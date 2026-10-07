package scenes;

import activities.CameraActivity;
import activities.KeyActivity;
import activities.LocalDbActivity;
import activities.UserActivity;
import exceptions.CancelledUserCreationException;
import exceptions.ConnectToWorksheetException;
import helpers.AlertUtils;
import helpers.CommonUtils;
import helpers.Constants;
import helpers.LoggingUtils;
import helpers.NumberRecognizer;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.NoRouteToHostException;
import java.util.logging.Level;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

public class GrizzlyScene {
  /** @author Dalton Smith GrizzlyScene Manages the main interface */

  // object that should be able to be modified by calling
  // this scene directly
  private static Label messageText = new Label("");

  private static TextField studentIDBox = new TextField();

  // define our scene objects
  private Button loginButton = new Button("Login/Logout");
  private UserActivity userActivity = new UserActivity();
  private Text description = new Text(Constants.kUserTutorial);
  private Hyperlink creditsLink = new Hyperlink("Credits");
  private Text creditsText = new Text("v" + Constants.kVersion);
  private Hyperlink optionsLink = new Hyperlink("Full Screen");
  private BorderPane bottomPane = new BorderPane();

  private AlertUtils alertUtils = new AlertUtils();

  private GridPane subRoot = new GridPane();

  // camera barcode scanner section, sits above the original interface
  private CameraActivity cameraActivity = new CameraActivity();
  private GridPane cameraPane = new GridPane();
  private StackPane cameraBox = new StackPane();
  private GridPane cameraControls = new GridPane();
  private Text cameraTitle = new Text(Constants.kCameraSectionTitle);
  private Label cameraStatus = new Label("");
  private CheckBox cameraToggle = new CheckBox(Constants.kCameraToggleLabel);
  private Button captureButton = new Button(Constants.kCaptureButtonLabel);
  private NumberRecognizer numberRecognizer = new NumberRecognizer();
  private Rectangle cameraGuide =
      new Rectangle(
          Constants.kCameraFeedWidth * Constants.kCameraGuideWidthFraction,
          Constants.kCameraFeedHeight * Constants.kCameraGuideHeightFraction);

  // boolean state variables
  private boolean handsFreeMode = LocalDbActivity.kHandsFreeMode;

  // our upper image
  private ImageView imageView;

  public GrizzlyScene() {
    Image splash;
    File file =
        new File(
            CommonUtils.getCurrentDir() + File.separator + "images" + File.separator + "error.png");

    // check for custom splash
    if (file.exists()) {
      splash = new Image(file.toURI().toString());

    } else {
      splash = new Image(Constants.kErrorImage);
    }

    imageView = new ImageView(splash);
  }

  public void updateInterface(GridPane root) {

    // camera scanner occupies the new top row, everything below keeps its
    // original order one row further down
    buildCameraSection();
    root.add(cameraPane, 0, 0);

    // create the upper image
    imageView.setFitHeight(Constants.kCameraHeight);
    imageView.setFitWidth(Constants.kCameraWidth);
    GridPane.setHalignment(imageView, HPos.CENTER);
    root.add(imageView, 0, 1);

    // update CSS IDS
    messageText.setId("messageText");
    studentIDBox.setId("textBox");
    loginButton.setId("confirmButton");
    creditsLink.setId("hyperlinkBottom");
    optionsLink.setId("hyperlinkBottom");
    creditsText.setId("hyperlinkBottom");

    // create our panes
    GridPane options = new GridPane();
    GridPane title = new GridPane();

    subRoot.setId("bottomView");

    // confirm alignments
    subRoot.setAlignment(Pos.CENTER);
    options.setAlignment(Pos.CENTER);
    title.setAlignment(Pos.CENTER);
    messageText.setAlignment(Pos.CENTER);
    description.setTextAlignment(TextAlignment.CENTER);
    description.setId("textDescription");

    // manually align message text because Gridpane is weird
    GridPane.setHalignment(messageText, HPos.CENTER);
    GridPane.setHalignment(description, HPos.CENTER);
    GridPane.setHalignment(subRoot, HPos.CENTER);

    // set bottom pane details
    bottomPane.setId("bottomPane");
    bottomPane.setLeft(optionsLink);
    bottomPane.setCenter(creditsText);
    bottomPane.setRight(creditsLink);
    bottomPane.setMinWidth(subRoot.getWidth());

    // add our various nodes to respective panes
    title.add(description, 0, 1);
    options.add(studentIDBox, 0, 0);
    options.add(loginButton, 1, 0);
    subRoot.add(title, 0, 0);
    subRoot.add(options, 0, 1);
    subRoot.add(messageText, 0, 2);

    // sub root details
    subRoot.setVgap(10);

    // add to root pane
    root.add(subRoot, 0, 2);
    root.add(bottomPane, 0, 3);

    // handle our buttons
    setEventHandlers();

    // open the camera last so a slow or missing device cannot hold up the interface
    startCameraScanner();

    // the scene is only reachable once this pane has been added to the root
    if (root.getScene() != null) {
      setCaptureShortcut(root.getScene());
    }
  }

  /** Builds the camera scanner section that sits above the original interface. */
  private void buildCameraSection() {
    cameraPane.setId("cameraView");
    cameraTitle.setId("cameraTitle");
    cameraStatus.setId("cameraStatus");
    cameraToggle.setId("cameraToggle");
    captureButton.setId("captureButton");
    captureButton.setDisable(true);

    cameraPane.setAlignment(Pos.CENTER);
    cameraPane.setVgap(5);

    // fixed size box so the layout does not jump when the first frame arrives
    cameraBox.setMinSize(Constants.kCameraFeedWidth, Constants.kCameraFeedHeight);
    cameraBox.setPrefSize(Constants.kCameraFeedWidth, Constants.kCameraFeedHeight);
    cameraBox.setMaxSize(Constants.kCameraFeedWidth, Constants.kCameraFeedHeight);
    cameraBox.setId("cameraBox");

    // marks the band that gets read, so it is obvious where to line the number
    // up. Sits beside the preview rather than inside it, so the mirrored
    // preview does not mirror the guide as well.
    cameraGuide.setFill(Color.TRANSPARENT);
    cameraGuide.setStroke(Color.web("#ffd400"));
    cameraGuide.setStrokeWidth(2);
    cameraGuide.setMouseTransparent(true);

    cameraBox.getChildren().addAll(cameraActivity.getCameraFrame(), cameraGuide);

    // hidden and unmanaged while off, so the window is not padded with an empty box
    cameraBox.setVisible(false);
    cameraBox.setManaged(false);

    GridPane.setHalignment(cameraTitle, HPos.CENTER);
    GridPane.setHalignment(cameraBox, HPos.CENTER);
    GridPane.setHalignment(cameraStatus, HPos.CENTER);
    GridPane.setHalignment(cameraControls, HPos.CENTER);
    GridPane.setHalignment(cameraPane, HPos.CENTER);

    cameraControls.setAlignment(Pos.CENTER);
    cameraControls.setHgap(12);
    cameraControls.add(cameraToggle, 0, 0);
    cameraControls.add(captureButton, 1, 0);

    cameraPane.add(cameraTitle, 0, 0);
    cameraPane.add(cameraBox, 0, 1);
    cameraPane.add(cameraControls, 0, 2);
    cameraPane.add(cameraStatus, 0, 3);
  }

  /** Wires the camera toggle and the capture control. */
  private void startCameraScanner() {
    cameraActivity.setOnStatus(status -> cameraStatus.setText(status));

    // Always starts off. The toggle is remembered for as long as the window is
    // open - switching to the credits screen and back keeps it - but a fresh
    // launch never opens the camera on its own.
    cameraToggle.setSelected(false);
    cameraStatus.setText(Constants.kCameraStatusOff);

    cameraToggle
        .selectedProperty()
        .addListener((observable, wasSelected, isSelected) -> setCameraEnabled(isSelected));

    captureButton.setOnAction(event -> captureId());
  }

  /**
   * Adds Space as a shortcut for the capture button.
   *
   * <p>An event filter rather than setOnKeyPressed, because KeyActivity already owns that handler
   * on the scene and setting it again would replace the full screen binding.
   */
  private void setCaptureShortcut(Scene scene) {
    scene.addEventFilter(
        KeyEvent.KEY_PRESSED,
        event -> {
          if (event.getCode() != KeyCode.SPACE) {
            return;
          }

          // a space is never part of an ID, so it is free to use as a shortcut
          // even while the ID box has focus
          event.consume();
          captureId();
        });
  }

  /**
   * Reads the ID number out of the frame currently on screen and types it into the ID box.
   *
   * <p>The number is not submitted automatically: recognising text off a card is less certain than
   * a barcode scan, so the user gets to see what was read before logging in.
   */
  private void captureId() {
    if (!cameraActivity.isRunning()) {
      return;
    }

    BufferedImage frame = cameraActivity.captureFrame();

    if (frame == null) {
      cameraStatus.setText(Constants.kCameraStatusStarting);
      return;
    }

    captureButton.setDisable(true);
    cameraStatus.setText(Constants.kCameraStatusReading);

    // recognition takes a few hundred milliseconds, which is far too long to
    // spend on the FX thread
    Runnable read =
        () -> {
          NumberRecognizer.Result result = numberRecognizer.read(frame);

          Platform.runLater(
              () -> {
                captureButton.setDisable(false);

                if (result == null) {
                  cameraStatus.setText(
                      numberRecognizer.isUnavailable()
                          ? Constants.kCameraStatusUnreadable
                          : Constants.kCameraStatusNoNumber);
                  return;
                }

                studentIDBox.setText(result.digits);
                studentIDBox.requestFocus();
                studentIDBox.positionCaret(result.digits.length());

                cameraStatus.setText(
                    result.validId
                        ? Constants.kCameraStatusScanning
                        : Constants.kCameraStatusCheck);
              });
        };

    Thread reader = new Thread(read, "capture-read");
    reader.setDaemon(true);
    reader.start();
  }

  /** Grows or shrinks the window by the preview height as the camera is switched on and off. */
  private void resizeStageForCamera(boolean cameraOn) {
    if (KeyActivity.isFullscreen || cameraToggle.getScene() == null) {
      return;
    }

    Stage stage = (Stage) cameraToggle.getScene().getWindow();

    if (stage == null) {
      return;
    }

    stage.setHeight(cameraOn ? Constants.kMainStageHeightCameraOn : Constants.kMainStageHeight);
    stage.centerOnScreen();
  }

  /** Turns the camera on or off for the rest of this session. */
  private void setCameraEnabled(boolean enabled) {
    cameraBox.setVisible(enabled);
    cameraBox.setManaged(enabled);
    captureButton.setDisable(!enabled);
    resizeStageForCamera(enabled);

    if (enabled) {
      cameraActivity.start();

    } else {
      cameraActivity.stop();
      cameraStatus.setText(Constants.kCameraStatusOff);
    }

    LoggingUtils.log(Level.INFO, "Camera " + (enabled ? "enabled" : "disabled"));
  }

  public void reShowUI(GridPane root) {
    root.setId("main");

    // add to root pane
    root.add(cameraPane, 0, 0);
    root.add(imageView, 0, 1);
    root.add(subRoot, 0, 2);
    root.add(bottomPane, 0, 3);
  }

  // our event handlers for interactivity
  private void setEventHandlers() {
    // login on enter key press
    studentIDBox.setOnAction(event -> confirmLogin());

    // login button event handler
    loginButton.setOnAction(event -> confirmLogin());

    creditsLink.setOnAction(event -> showCredits());

    optionsLink.setOnAction(
        event -> {
          Stage stage = (Stage) optionsLink.getScene().getWindow();
          if (KeyActivity.isFullscreen) {
            stage.setFullScreen(false);
            KeyActivity.isFullscreen = false;
          } else {
            stage.setFullScreen(true);
            KeyActivity.isFullscreen = true;
          }
        });
  }

  private void showCredits() {
    SceneManager.updateScene(Constants.kCreditsSceneState);
  }

  // helper login method
  private void confirmLogin() {
    setMessageBoxText("Processing...");

    /* Brooks - 2023/24 Season Changes
    As of the 2023 school year the school's bar code system has changed to append a an alpa char at the beginning and
    end of the scanned ID bar code.  To ameliorate this strip and non-alpha chars entered into the text box at
    login time.
    */
    studentIDBox.setText(studentIDBox.getText().replaceAll("[^0-9.]", ""));

    // confirm the ID is vslid
    if (!userActivity.isValidID(studentIDBox.getText())) {
      setMessageBoxText("ID " + studentIDBox.getText() + " is invalid.");

      Task<Void> wait =
          new Task<Void>() {
            @Override
            protected Void call() throws Exception {
              Thread.sleep(5000);
              return null;
            }
          };

      wait.setOnSucceeded(e -> setMessageBoxText(""));

      // no need to set as daemon as will end after x seconds.
      new Thread(wait).start();
      return;
    }

    if (!handsFreeMode) {
      // confirm that the user wants to login/logout
      if (alertUtils.confirmInput("Confirm login/logout of user: " + studentIDBox.getText())) {
        loginUser();
      } else {
        setMessageBoxText("");
      }

      // show no prompts
    } else {
      loginUser();
    }
  }

  // login the user, check if hands free or not
  private void loginUser() {
    // separate login process on different thread to ensure
    // main application does not freeze
    // also allows in for multiple users login simultaneously
    Runnable loginUser =
        () -> {
          // ensure that the user typed something in
          if (studentIDBox.getText().isEmpty()) {
            setMessageBoxText("Nothing was entered!");
            return;
          }

          // attempt login/logout and or account creation
          // do nothing if account creation was cancelled
          try {
            // check if the user is logged in, and that user exists
            if (!(userActivity.isUserLoggedIn(studentIDBox.getText()))) {
              LoggingUtils.log(Level.INFO, "Logging in: " + studentIDBox.getText());
              userActivity.loginUser(studentIDBox.getText());

            } else {
              LoggingUtils.log(Level.INFO, "Logging out: " + studentIDBox.getText());
              userActivity.logoutUser(studentIDBox.getText());
            }

          } catch (CancelledUserCreationException e) {
            setMessageBoxText("Cancelled account creation");

          } catch (ConnectToWorksheetException e) {
            setMessageBoxText("There was an error connecting to the database. Please retry.");

          } catch (NoRouteToHostException e) {
            setMessageBoxText("Unable to connect to database. Check internet and retry.");

          } catch (Exception e) {
            LoggingUtils.log(Level.SEVERE, e);
            setMessageBoxText("An unknown error has occurred, see log file.");
          }

          // refocus the textbox
          Platform.runLater(() -> studentIDBox.requestFocus());
        };

    // start our thread
    Thread t = new Thread(loginUser);
    t.setDaemon(true);
    t.start();
  }

  // helper methods for setting and clearing text box
  public static void setMessageBoxText(String text) {
    if (Platform.isFxApplicationThread()) {
      messageText.setText(text);
    } else {
      Platform.runLater(() -> messageText.setText(text));
    }
  }

  public static void clearInput() {
    studentIDBox.clear();
  }
}
