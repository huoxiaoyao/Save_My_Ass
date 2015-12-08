package ch.ethz.inf.vs.a4.savemyass.Centralized;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;

import ch.ethz.inf.vs.a4.savemyass.Structure.AlarmSender;
import ch.ethz.inf.vs.a4.savemyass.Structure.InfoBundle;

/**
 * Created by jan on 01.12.15.
 *
 * sends a request to the app engine server that starts an alarm
 *
 * todo: how exactly should we propagate the alarm ID and stuff back to the activity to be conform with p2p approach?
 */
public class GCMSender implements AlarmSender, ResponseListener {

    private final static String TAG = "###GCMSender";

    private Context ctx;
    private LocationTracker locationTracker;

    public GCMSender(Context ctx, LocationTracker locationTracker){
        this.ctx = ctx;
        this.locationTracker = locationTracker;
    }

    @Override
    public void callForHelp(InfoBundle bundle) {
        Log.d(TAG, "sending call for help to server");
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);
        String userID = sharedPreferences.getString(Config.SHARED_PREFS_USER_ID, "");
        InfoBundle info = new InfoBundle(userID, locationTracker.loggedLocation);
        JSONObject request = info.toJSON();
        if(request == null){
            Toast toast = Toast.makeText(ctx, "Location is null! Running on a device without google play services?", Toast.LENGTH_SHORT);
            toast.show();
            return;
        }
        RequestSender requestSender = new RequestSender(Config.GCM_START_ALARM_URL);
        requestSender.sendRequest(request, this);
    }

    @Override
    public void onResponseReceive(JSONObject response) {

        if(response == null){
            Log.d(TAG, "server responded with null");
            Toast toast = Toast.makeText(ctx, "server responded with null", Toast.LENGTH_SHORT);
            toast.show();
            return;
        }
        Log.d(TAG, "response: "+response.toString());

        //todo: do appropriate error handling here!
        if(response.has("error")) {
            Log.d(TAG, "couldn't trigger alarm");
        }
        else {
            Log.d(TAG, "successfully triggered the alarm!");
        }
    }
}
