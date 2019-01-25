import exceptions.OpenCvLoadFailureException;
import helpers.*;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import notifiers.UpdateNotifier;
import scenes.SplashScene;
import tasks.CameraStream;
import tasks.KeyHandlers;
import tasks.UserInterface;

import java.io.File;
import java.util.logging.Level;

public class GrizzlyTime extends Application {
    /**
     * @author Dalton Smith
     * GrizzlyTime main application class
     * This class calls our various tasks and starts the JavaFX application
     */

    //only initializations that don't have freezing constructor instances should be placed here
    private SplashScene splash = new SplashScene();
    private KeyHandlers keyHandlers = new KeyHandlers();
    private AlertUtils alertUtils = new AlertUtils();
    private UpdateNotifier updater = new UpdateNotifier();

    @Override
    public void start(Stage primaryStage) {

        Thread.setDefaultUncaughtExceptionHandler(GrizzlyTime::globalExceptionHandler);
        //grab our application icon from stream

        //check if custom icon
        File file = new File(CommonUtils.getCurrentDir() + "\\images\\icon.png");

        if (file.exists()) {
            primaryStage.getIcons().add(new Image(file.toURI().toString()));

        } else {
            primaryStage.getIcons().add(new Image(this.getClass().getResourceAsStream(Constants.kApplicationIcon)));

        }

        GridPane root = new GridPane();

        Scene scene = new Scene(root, Constants.kSplashWidth, Constants.kSplashHeight);
        scene.getStylesheets().add(Constants.kRootStylesheet);

        root.setId("main");
        root.setAlignment(Pos.CENTER);

        primaryStage.setTitle(Constants.kApplicationName);
        primaryStage.setScene(scene);
        primaryStage.setResizable(Constants.kWindowResizable);

        //show our splash
        splash.showSplash(root);
        primaryStage.show();
        primaryStage.requestFocus();

        //initialize our tasks and interface objects AFTER
        //we display application
        UserInterface userInterface = new UserInterface();
        CameraStream processor = null;

        if(System.getProperty("os.name").toLowerCase().contains("mac")){
            alertUtils.createAlert("Unsupported", "Mac OS not supported", "Mac OS is not supported at this time, running in experimental mode");
        } else {
            //copy OpenCV dlls outside jar
            try {
                CVHelper.loadLibrary();
                processor = new CameraStream();

            } catch (OpenCvLoadFailureException e) {
                //do nothing

            }
        }

        AlertUtils.stage = primaryStage;

        //remove splash screen on load
        root.getChildren().clear();
        primaryStage.setWidth(Constants.kMainStageWidth);
        primaryStage.setHeight(Constants.kMainStageHeight);
        primaryStage.centerOnScreen();

        //add our global key handlers
        keyHandlers.setKeyHandlers(scene, primaryStage);

        //process camera frames and read barcode images
        if (processor != null) {
            processor.displayImage(root);
        }

        //check for updates
        updater.checkUpdates();

        //create UI and logic
        userInterface.updateInterface(root);

    }

    //catch uncaught exceptions
    private static void globalExceptionHandler(Thread thread, Throwable throwable) {
        LoggingUtils.log(Level.SEVERE, throwable);
        CommonUtils.exitApplication();
    }
}
