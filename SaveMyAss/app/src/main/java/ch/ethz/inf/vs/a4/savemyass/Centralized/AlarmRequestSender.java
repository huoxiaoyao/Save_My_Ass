package ch.ethz.inf.vs.a4.savemyass.Centralized;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import ch.ethz.inf.vs.a4.savemyass.HelperMapCombiner;


/**
 * Created by jan on 12.12.15.
 *
 * sends alarm when the user triggered it
 */
public class AlarmRequestSender extends AbstractAlarmRequestSender implements ResponseListener {

    private HelperMapCombiner mapCombiner;
    public AlarmRequestSender(Context ctx, HelperMapCombiner mapCombiner) {
        super(ctx);
        setResponseListener(this);
        this.mapCombiner = mapCombiner;
    }


    @Override
    public void onResponseReceive(JSONObject json) {
        if(json.has("error")) {
            Log.d(TAG, "couldn't trigger alarm");
        }
        else {
            try {
                String firebaseUrl = json.getString("node");
                firebaseUrl = Config.FIREBASE_BASE_ADDRESS+"alarms/"+firebaseUrl+"/";
                firebaseUrl = firebaseUrl+"helpers/";
                new OnGoingAlarmPIN(ctx, firebaseUrl, infoBundle, mapCombiner);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
