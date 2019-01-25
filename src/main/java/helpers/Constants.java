package helpers;

public class Constants {
    //spreadsheet layout
    public static final int kLoggedInColumn = 10;
    public static final int kTotalHoursColumn = 9;
    public static final int kHoursColumn = 8;
    public static final int kLastLogoutColumn = 7;
    public static final int kLastLoginColumn = 6;
    public static final int kStudentIdColumn = 0;
    public static final int kFirstNameColumn = 1;
    public static final int kLastNameColumn = 2;

    //camera feed size
    public static final int kCameraHeight = 400;
    public static final int kCameraWidth = 610;

    //window constants
    public static final boolean kWindowResizable = false;

    //sheet codes
    public static final int kMainSheet = 0;
    public static final int kLogSheet = 1;
    public static final int kRegistrationSheet = 2;

    //configuration locations
    public static final String kConfigName = "config.json";

    //resource locations
    public static final String kRootStylesheet = "styles/root.css";
    public static final String kApplicationIcon = "images/icon.png";
    public static final String kBearImage = "images/bear.png";
    public static final String kErrorImage = "images/error.png";
    public static final String kSplashImage = "images/splash.jpg";

    //alert ui constants
    public static final int kBearImageWidth = 50;
    public static final boolean kBearPreserveRatio = true;
    public static final int kWordWrapWidth = 400;

    //main ui constants
    public static final String kUserTutorial = "Type in your Student ID to login. If you do not have a Student ID," +
            "\nenter your birth date in 8 digits. [MMDDYYYY]";
    public static final int kMainStageWidth = 608;
    public static final int kMainStageHeight = 630;
    public static final String kApplicationName = "GrizzlyTime JavaFX Edition";

    //credits ui constants
    public static final String kCreditsSummary = "" +
            "GrizzlyTime is a JavaFX application programmed originally for FRC Team 66, Grizzly Robotics. " +
            "GrizzlyTime was programmed by Grizzly Robotics Team Captain of Year 2018/2019, Dalton Smith. " +
            "All rights and permissions are reserved. Content is licensed under MIT. See below for more information.";
    public static final String kCreditsList = "GrizzlyTime uses the following open source projects:\n" +
            "OpenCV 3.4.3\n" +
            "Google Java API Client 1.23.0\n" +
            "ZXIng Barcode Scanning Library 3.3.0\n" +
            "Commons-IO 2.6\n" +
            "Org.Json";
    public static final int kCreditsStageWidth = 630;
    public static final int kCreditsStageHeight = 550;
    public static final int kCreditsBearHeight = 225;
    public static final boolean kCreditsBearPreserveRatio = true;
    public static final int kCreditsWrapTextWidth = 285;

    public static final String kVersion = "2.0.0";

    //splash constants
    public static final int kSplashWidth = 390;
    public static final int kSplashHeight = 190;

    //grizzly robotics specific
    public static final boolean kGrizzlyPrompt = false;
    public static final boolean kMentorFallback = true;

    public static final String kUpdateUrl = "https://raw.githubusercontent.com/Daltz333/GrizzlyTime-JavaFX/master/version.txt";
    public static final String kReleaseUrl = "https://github.com/Daltz333/GrizzlyTime-JavaFX/releases/latest";


}
