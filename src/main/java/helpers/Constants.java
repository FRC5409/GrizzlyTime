package helpers;

import activities.LocalDbActivity;

public class Constants {
  // spreadsheet layout
  public static final int kEthnicityColumn = 11;
  public static final int kLoggedInColumn = 10;
  public static final int kTotalHoursColumn = 9;
  public static final int kHoursColumn = 8;
  public static final int kLastLogoutColumn = 7;
  public static final int kLastLoginColumn = 6;
  public static final int kStudentIdColumn = 0;
  public static final int kFirstNameColumn = 1;
  public static final int kLastNameColumn = 2;
  public static final int kGenderColumn = 5;
  public static final int kRoleColumn = 4;
  public static final int kEmailColumn = 3;

  // camera feed size
  public static final int kCameraHeight = 300;
  public static final int kCameraWidth = 500;

  // barcode camera section
  // Size the live preview is drawn at. 16:9 to match the capture sizes below,
  // so the preview is not letterboxed and shows exactly what is being scanned.
  public static final int kCameraFeedWidth = 400;
  public static final int kCameraFeedHeight = 225;

  // The guide box drawn on the preview, as a fraction of the frame, and exactly
  // the area that gets read. A thin band so it frames one line of text - the ID
  // number on its own, without the rest of the card - but nearly full width:
  // a narrower band would slice the ends off a number wider than the box and
  // hand back a shorter number that still looks like a valid ID.
  public static final double kCameraGuideWidthFraction = 0.96;
  public static final double kCameraGuideHeightFraction = 0.18;

  // Sizes to capture at, best first. Far larger than the preview on purpose:
  // the bars on a student ID are narrow, and at VGA a card that does not nearly
  // fill the frame does not carry enough pixels per bar to decode.
  public static final int[][] kCameraCaptureSizes = {{1920, 1080}, {1280, 720}, {640, 480}};

  // pause between preview frames, about 20 a second
  public static final int kCameraScanIntervalMs = 50;

  // reading the number happens on demand, not on every frame
  public static final String kTessdataFolder = "tessdata";
  public static final String kCaptureLanguage = "eng";

  // treat the guide area as a block of text rather than forcing a single line
  public static final int kCapturePageSegMode = 6;

  // crops shorter than this are scaled up, which Tesseract reads far better
  public static final int kCaptureMinHeight = 300;

  public static final String kCameraSectionTitle = "Camera Scanner";
  public static final String kCameraToggleLabel = "Enable camera";
  public static final String kCaptureButtonLabel = "Capture ID (Space)";
  public static final String kCameraStatusOff = "Camera is off";
  public static final String kCameraStatusStarting = "Starting camera...";
  public static final String kCameraStatusScanning =
      "Line the ID number up inside the box, then press Capture or Space";
  public static final String kCameraStatusReading = "Reading...";
  public static final String kCameraStatusNoNumber = "No number found - line it up and try again";
  public static final String kCameraStatusCheck = "Check the number before logging in";
  public static final String kCameraStatusUnreadable = "Text recognition unavailable, see log file";
  public static final String kCameraStatusDisabled = "Camera scanning is disabled";
  public static final String kCameraStatusUnavailable =
      "No camera available - use the scanner or type your ID";

  // window constants
  public static final boolean kWindowResizable = false;

  // sheet codes
  public static final int kMainSheet = 0;
  public static final int kLogSheet = 1;
  public static final int kRegistrationSheet = 2;

  // configuration locations
  public static final String kConfigName = "config.json";

  // resource locations
  // resources inside a jar path seperator are NOT OS dependent
  public static final String kRootStylesheet = "styles/root.css";
  public static final String kApplicationIcon = "images/icon.png";
  public static final String kBearImage = "images/bear.png";
  public static final String kErrorImage = "images/error.png";
  public static final String kSplashImage = "images/splash.png";

  // alert ui constants
  public static final int kBearImageWidth = 50;
  public static final boolean kBearPreserveRatio = true;
  public static final int kWordWrapWidth = 400;

  // main ui constants
  public static final String kUserTutorial =
      "Type in your Student ID to login. If you do not have a Student ID,"
          + "\nenter any "
          + LocalDbActivity.kIdLengthFallback
          + " digit ID number.";
  public static final int kMainStageWidth = 608;

  // the camera section's title, toggle and status line always sit above the
  // original layout, so the window is this tall with the camera switched off
  public static final int kMainStageHeight = 630 + 75;

  // the live preview is only laid out while the camera is on, and the window
  // grows by exactly that much to show it
  public static final int kMainStageHeightCameraOn = kMainStageHeight + kCameraFeedHeight + 10;
  public static final String kApplicationName = "GrizzlyTime JavaFX Edition";

  // credits ui constants
  public static final String kCreditsSummary =
      ""
          + "GrizzlyTime is a JavaFX application programmed originally for FRC Team 66, Grizzly Robotics. "
          + "GrizzlyTime was programmed by Grizzly Robotics Team Captain of Year 2018/2019, Dalton Smith. "
          + "All rights and permissions are reserved. Content is licensed under MIT. See below for more information.";
  public static final String kCreditsList =
      "GrizzlyTime uses the following open source projects:\n"
          + "Google Java API Client 1.23.0\n"
          + "Commons-IO 2.6\n"
          + "Org.Json\n"
          + "Webcam Capture 0.3.12\n"
          + "ZXing 3.5.3";
  public static final int kCreditsStageWidth = 630;
  public static final int kCreditsStageHeight = 550;
  public static final int kCreditsBearHeight = 225;
  public static final boolean kCreditsBearPreserveRatio = true;
  public static final int kCreditsWrapTextWidth = 285;

  public static final String kVersion = "2.4.0";

  // splash constants
  public static final int kSplashWidth = 390;
  public static final int kSplashHeight = 190;

  // grizzly robotics specific
  public static final boolean kMentorFallback = true;

  public static final String kUpdateUrl =
      "https://raw.githubusercontent.com/YCSRobotics/GrizzlyTime/main/version.txt";
  public static final String kReleaseUrl =
      "https://github.com/YCSRobotics/GrizzlyTime/wiki/5.-Updating-GrizzlyTime";

  // Accepted ID lengths beyond the short school ID and the mentor fallback.
  // Cards here also carry a long number under the barcode, and that is just as
  // valid an ID as the short one.
  public static final int kLongIdMinLength = 14;
  public static final int kLongIdMaxLength = 16;

  // user states
  public static final int kIdDoesNotExist = -1;
  public static final int kIdLoggedIn = 0;
  public static final int kIdNotLoggedIn = 1;

  // scene manager states
  public static final int kSplashSceneState = 0;
  public static final int kMainSceneState = 1;
  public static final int kCreditsSceneState = 2;
  public static final int kOptionsSceneState = 3;
  public static final int kLoadMainScene = 4;
}
