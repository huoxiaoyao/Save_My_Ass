package ch.ethz.inf.vs.a4.savemyass.Centralized;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.firebase.client.FirebaseError;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;

import ch.ethz.inf.vs.a4.savemyass.Structure.PINInfoBundle;
import ch.ethz.inf.vs.a4.savemyass.UI.AlarmNotifier;

/**
 * Created by jan on 09.12.15.
 *
 * The service for a potential helper.
 *
 * - notifies the user if necessary
 * - enlarges the radius if nobody replied within an amount of time etc.
 *
 */
public class OnGoingAlarmWatchdog extends OnGoingAlarm {

    public OnGoingAlarmWatchdog(Context ctx, String firebaseURL, PINInfoBundle pinInfoBundle){
        super(ctx, firebaseURL, pinInfoBundle);
    }

    @Override
    void onGeoFireRefReady() {
        //todo: implement logic
        GeoQuery geoQuery = geoFire.queryAtLocation(pinLocation, Config.ALARM_DISTANCE_THRESHOLD);
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                Log.d(TAG, "enter - key: " + key + " loc: " + location.latitude + "," + location.longitude);
            }

            @Override
            public void onKeyExited(String key) {
                Log.d(TAG, "exit - key: " + key);
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                Log.d(TAG, "move - key: " + key + " loc: " + location.latitude + "," + location.longitude);
            }

            @Override
            public void onGeoQueryReady() {
                Log.d(TAG, "All initial data has been loaded and events have been fired!");
            }

            @Override
            public void onGeoQueryError(FirebaseError error) {
                Log.d(TAG, "There was an error with this query: " + error);
            }
        });
        startAlarm();
    }

    // shows the notification and starts alarm
    private void startAlarm() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        sp.edit().putString(Config.INTENT_FIREBASE_ALARM_URL, firebaseURL).apply();
        AlarmNotifier ai = new AlarmNotifier(ctx);
        ai.callForHelp(pinInfoBundle);
    }
}
