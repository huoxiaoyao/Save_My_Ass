package ch.ethz.inf.vs.a4.savemyass.Centralized;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

import ch.ethz.inf.vs.a4.savemyass.Structure.AlarmDistributor;
import ch.ethz.inf.vs.a4.savemyass.Structure.PINInfoBundle;

/**
 * Created by jan on 30.11.15.
 *
 * receives the google cloud message and starts the OnGoingAlarmService that then monitors the
 * current alarm.
 * Note that all the people that are somewhat near the person in need of help get the GCM message.
 */
//todo: implement logic
public class GCMReceiver extends GcmListenerService {

    private AlarmDistributor uiNotifier;
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
        String firebaseUrl = data.getString("firebase_url");
        // todo: only until simon implemented this
        firebaseUrl = "-K5ABjBl8nO5WH5LN3gw";
        firebaseUrl = Config.FIREBASE_BASE_ADDRESS+firebaseUrl+"/";
        Log.d(TAG, "From: " + from);
        Log.d(TAG, "Bundle: " + data);


        //String userID = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);

        // start the watchdog
        Intent i = new Intent(Config.LOCAL_BROADCAST_HELPER_ALARM_INFO);
        i.putExtra(Config.INTENT_FIREBASE_ALARM_URL, firebaseUrl);
        Location pinLocation = new Location("");
        pinLocation.setLatitude(Double.parseDouble(data.getString("latitude")));
        pinLocation.setLongitude(Double.parseDouble(data.getString("longitude")));
        PINInfoBundle infoBundle = new PINInfoBundle(data.getString("user_id"), pinLocation, data.getString("msg"));
        i.putExtra(Config.INTENT_INFO_BUNDLE, infoBundle);
        //todo: this broadcast doesn't get received yet
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);
    }
}
