package ch.ethz.inf.vs.a4.savemyass.Centralized;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import ch.ethz.inf.vs.a4.savemyass.Structure.AlarmSender;
import ch.ethz.inf.vs.a4.savemyass.Structure.PINInfoBundle;

/**
 * Created by jan on 01.12.15.
 *
 * sends a request to the app engine server that starts an alarm
 */
public class GCMSender implements AlarmSender, ResponseListener {

    private final static String TAG = "###GCMSender";

    private Context ctx;
    private LocationTracker locationTracker;
    private PINInfoBundle infoBundle;

    public GCMSender(Context ctx, LocationTracker locationTracker){
        this.ctx = ctx;
        this.locationTracker = locationTracker;
    }

    @Override
    public void callForHelp(PINInfoBundle bundle) {
        if(bundle.loc == null){
            Toast toast = Toast.makeText(ctx, "Location is null! Running on a device without google play services?", Toast.LENGTH_SHORT);
            toast.show();
            return;
        }
        infoBundle = bundle;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        JSONObject request = bundle.toJSON();
        try {
            request.put("salt", sp.getString(Config.SHARED_PREFS_SALT, ""));
            RequestSender requestSender = new RequestSender(Config.GCM_START_ALARM_URL);
            Log.d(TAG, "sending call for help to server: "+request.toString());
            requestSender.sendRequest(request, this);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResponseReceive(JSONObject response) {
        if(response == null){
            Log.d(TAG, "server responded with null");
            Toast toast = Toast.makeText(ctx, "server responded with null", Toast.LENGTH_SHORT);
            toast.show();
            return;
        }

        //todo: do appropriate error handling here!

        //todo: implement the case where we want to send someone else's alarm further on (got to us by p2p)
        if(response.has("error")) {
            Log.d(TAG, "couldn't trigger alarm");
        }
        else {
            try {
                String url = response.getString("node");
                Intent i = new Intent(Config.LOCAL_BROADCAST_PIN_ALARM_INFO);
                i.putExtra(Config.INTENT_FIREBASE_ALARM_URL, Config.FIREBASE_BASE_ADDRESS+url+"/");
                i.putExtra(Config.INTENT_INFO_BUNDLE, infoBundle);
                LocalBroadcastManager.getInstance(ctx.getApplicationContext()).sendBroadcast(i);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "successfully triggered the alarm!");
        }
    }
}
