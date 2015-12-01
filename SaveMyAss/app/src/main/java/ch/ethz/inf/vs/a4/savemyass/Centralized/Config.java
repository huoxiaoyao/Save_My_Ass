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
    public static String GCM_REGISTER = "https://save-my-ass.appspot.com/_ah/api/savemyass/v1/register";
    public static String GCM_START_ALARM = "https://save-my-ass.appspot.com/_ah/api/savemyass/v1/alarm";

    // time and distance thresholds
    // todo: replace those with reasonable values!
    public static int LOCATION_UPDATE_PERIOD = 20*1000; //in ms -> maximum time between two location updates
    public static int LOCATION_UPDATE_PERIOD_MIN = 10*1000; //in ms -> we won't get more location updates than that
    public static float LOCATION_TRACKER_SEND_DISTANCE_THRESHOLD = 1000; //in m
}
