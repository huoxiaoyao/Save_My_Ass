package ch.ethz.inf.vs.a4.savemyass.Centralized;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.Settings;
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
public abstract class AbstractAlarmRequestSender implements AlarmSender{

    protected final static String TAG = "###AlarmRequestSender";

    protected Context ctx;
    protected PINInfoBundle infoBundle;
    private ResponseListener responseListener;

    public AbstractAlarmRequestSender(Context ctx){
        this.ctx = ctx;
    }

    public void setResponseListener(ResponseListener newResponseListener){
        responseListener = newResponseListener;
    }

    @Override
    public void callForHelp(PINInfoBundle bundle) {
        if(bundle.loc == null){
            Toast toast = Toast.makeText(ctx, "Location is null! Running on a device without google play services?", Toast.LENGTH_SHORT);
            toast.show();
            //return;
            bundle.loc = Config.DUMMY_LOC();
        }
        infoBundle = bundle;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        bundle.userID = Settings.Secure.getString(ctx.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        JSONObject request = bundle.toJSON();
        try {
            request.put("salt", sp.getString(Config.SHARED_PREFS_SALT, ""));
            RequestSender requestSender = new RequestSender(Config.GCM_START_ALARM_URL);
            Log.d(TAG, "sending call for help to server: "+request.toString());
            requestSender.sendRequest(request, responseListener);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
