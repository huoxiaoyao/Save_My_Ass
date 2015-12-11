package ch.ethz.inf.vs.a4.savemyass.Centralized;

/**
 * Created by jan on 30.11.15.
 *
 * contains some configuration stuff for the server based approach
 */
public class Config {
    //firebase stuff
    public static String FIREBASE_BASE_ADDRESS = "https://savemya.firebaseio.com/";
    public static String FIREBASE_LOCATION_TRACKING = FIREBASE_BASE_ADDRESS+"locations/";

    // google cloud messaging stuff
    public static String GCM_REGISTER_URL = "https://save-my-ass.appspot.com/_ah/api/savemyass/v1/register";
    public static String GCM_START_ALARM_URL = "https://save-my-ass.appspot.com/_ah/api/savemyass/v1/alarm";
    public static String REGISTRATION_COMPLETE_BROADCAST = "registrationComplete";

    // shared preferences
    public static String SENT_TOKEN_TO_SERVER = "sentTokenToServer";
    public static String SHARED_PREFS_TOKEN = "gcm_token";
    public static String SHARED_PREFS_SALT = "gcm_salt";
    public static String SHARED_PREFS_FIREBASE_AUTH = "firebase_auth";
    public static String SHARED_PREFS_USER_MESSAGE = "user_message";

    // Intent extra strings
    public static String INTENT_NEW_TOKEN = "newToken";
    public static String INTENT_FIREBASE_ALARM_URL = "firebaseAlarmUrl";
    public static String INTENT_INFO_BUNDLE = "alarmInfoBundle";

    // Broadcasts
    public static String LOCAL_BROADCAST_PIN_ALARM_INFO = "pintAlarm";
    public static String LOCAL_BROADCAST_HELPER_ALARM_INFO = "pintAlarm";
    public static String LOCAL_BROADCAST_START_ALARM = "start_alarm";

    // time and distance thresholds
    // todo: replace those with reasonable values!
    public static int LOCATION_TRACKER_UPDATE_PERIOD = 20*1000; //in ms -> maximum time between two location updates
    public static int LOCATION_TRACKER_UPDATE_PERIOD_MIN = 10*1000; //in ms -> we won't get more location updates than that
    public static float LOCATION_TRACKER_SEND_DISTANCE_THRESHOLD = 100; //in m
    public static int ALARM_DISTANCE_THRESHOLD = 5*1000; //in m
    public static int MIN_PERIOD_BETWEEN_TWO_ALARMS = 10 * 60 * 1000; // in ms -> this only applies for the same person
    public static int IGNORE_OTHER_LOCATION_THRESHOLD = 10 * 1000; // in ms

    public static String MODE = "mode";
    public enum BackgroundServiceStartMode{
        ALARM_PIN, ALARM_HELPER, NORMAL
    }
}
