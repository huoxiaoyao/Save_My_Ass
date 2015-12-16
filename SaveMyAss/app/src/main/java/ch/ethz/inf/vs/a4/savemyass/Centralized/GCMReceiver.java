package ch.ethz.inf.vs.a4.savemyass.Centralized;

import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

import ch.ethz.inf.vs.a4.savemyass.Structure.PINInfoBundle;

/**
 * Created by jan on 30.11.15.
 *
 * receives the google cloud message and starts the OnGoingAlarmService that then monitors the
 * current alarm.
 * Note that all the people that are somewhat near the person in need of help get the GCM message.
 */
public class GCMReceiver extends GcmListenerService {

    protected static String TAG = "###GCMReceiver";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onMessageReceived(String from, Bundle data) {
        super.onMessageReceived(from, data);
        String firebaseUrl = data.getString("node");
        firebaseUrl = Config.FIREBASE_BASE_ADDRESS+"alarms/"+firebaseUrl+"/helpers/";
        Log.d(TAG, "From: " + from);
        Log.d(TAG, "Bundle: " + data);

        // todo: do error handling here!

        Location pinLocation = new Location("");
        pinLocation.setLatitude(Double.parseDouble(data.getString("latitude")));
        pinLocation.setLongitude(Double.parseDouble(data.getString("longitude")));
        PINInfoBundle infoBundle = new PINInfoBundle(data.getString("user"), pinLocation, data.getString("msg"));


        // start the watchdog
        String myUserID = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        if(myUserID != null) {
            if (sp.getBoolean(Config.SHARED_PREFS_CENTRALIZED_ACTIVE, true) && !infoBundle.userID.equals(myUserID))
                new OnGoingAlarmWatchdog(getApplicationContext(), firebaseUrl, infoBundle);
        }
    }
}
