package ch.ethz.inf.vs.a4.savemyass.Centralized;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

import ch.ethz.inf.vs.a4.savemyass.Structure.AlarmDistributor;
import ch.ethz.inf.vs.a4.savemyass.Structure.InfoBundle;

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
        Log.d(TAG, "got a new GCM message");
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "destroy");
        super.onDestroy();
    }

    @Override
    public void onMessageReceived(String from, Bundle data) {
        super.onMessageReceived(from, data);
        String firebaseUrl = data.getString("firebase_url");
        String message = data.getString("msg");
        Log.d(TAG, "From: " + from);
        Log.d(TAG, "Message: " + message);
        //Log.d(TAG, "Firebase URL: " + firebaseUrl);
        // todo: in case we need parts of the message for the UI part, do that here

        // start the service which tracks the state of the alarm
        Intent i = new Intent(getApplicationContext(), OnGoingAlarmService.class);
        i.putExtra(Config.INTENT_FIREBASE_ALARM_URL, firebaseUrl);
        startService(i);



        InfoBundle info = new InfoBundle("implement this!", null);
        //uiNotifier.distributeToSend(info);
    }
}
