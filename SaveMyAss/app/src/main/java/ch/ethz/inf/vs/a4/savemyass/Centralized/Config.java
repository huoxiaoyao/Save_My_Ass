package ch.ethz.inf.vs.a4.savemyass.Centralized;

import android.location.Location;

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
    public static String SHARED_PREFS_CENTRALIZED_ACTIVE = "centralized_active";
    public static String SHARED_PREFS_P2P_ACTIVE = "p2p_active";
    public static String SHARED_PREFS_FIRSTOPEN = "firstopen";
    public static String SHARED_PREFS_ALARM_ACTIVE = "alarm_active";

    // Intent extra strings
    public static String INTENT_NEW_TOKEN = "newToken";
    public static String INTENT_FIREBASE_ALARM_URL = "firebaseAlarmUrl";
    public static String INTENT_INFO_BUNDLE = "alarmInfoBundle";

    // time and distance thresholds
    public static int LOCATION_TRACKER_UPDATE_PERIOD = 60*1000; //in ms -> maximum time between two location updates
    public static int LOCATION_TRACKER_UPDATE_PERIOD_MIN = 30*1000; //in ms -> we won't get more location updates than that
    public static float LOCATION_TRACKER_SEND_DISTANCE_THRESHOLD = 100; //in m -> we don't update the location in firebase if the delta is smaller than this.
    public static int ALARM_DISTANCE_THRESHOLD = 5; //in km - the distance to the PIN in which the watchdog might consider triggering an alarm
    public static int IGNORE_OTHER_LOCATION_THRESHOLD = 10 * 1000; // in ms
    public static int WATCHDOG_RESPOND_COUNT_THRESHOLD = 2; //so many people need to accept before we stop increasing the radius...
    public static double[] WATCHDOG_RADIUS = {0.25, 0.5, 1, ALARM_DISTANCE_THRESHOLD};//the steps that the watchdog makes when increasing the radius around the person in need
    public static int[] WATCHDOG_TIMES = {10, 15, 15}; //after the first element seconds the radius will be increased to the second radius value above and so on...

    // dummy location used for triggering alarms in the emulator
    public static Location DUMMY_LOC() {
        Location loc = new Location("");
        loc.setLatitude(47.411492);
        loc.setLongitude(8.544242);
        return loc;
    }
}
