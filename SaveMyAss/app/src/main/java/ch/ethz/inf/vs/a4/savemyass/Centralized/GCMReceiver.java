package ch.ethz.inf.vs.a4.savemyass.Centralized;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

import ch.ethz.inf.vs.a4.savemyass.Structure.AlarmDistributor;
import ch.ethz.inf.vs.a4.savemyass.Structure.InfoBundle;

/**
 * Created by jan on 30.11.15.
 *
 * receives the google cloud message and contains logic when and trigger the alarm
 */
//todo: implement logic
public class GCMReceiver extends GcmListenerService{

    private AlarmDistributor uiNotifier;
    protected static String TAG = "###GCMReceiver";

    public GCMReceiver(AlarmDistributor uiNotifier){
        this.uiNotifier = uiNotifier;
    }


    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onMessageReceived(String from, Bundle data) {
        super.onMessageReceived(from, data);
        String message = data.getString("message");
        Log.d(TAG, "From: " + from);
        Log.d(TAG, "Message: " + message);
        // todo: in case we need parts of the message for the UI part, do that here
        InfoBundle info = new InfoBundle();
        uiNotifier.distributeToSend(info);
    }
}
