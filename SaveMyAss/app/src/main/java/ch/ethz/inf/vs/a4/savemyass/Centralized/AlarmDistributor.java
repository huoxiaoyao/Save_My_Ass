package ch.ethz.inf.vs.a4.savemyass.Centralized;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by jan on 11.12.15.
 *  
 * alarm got to us by p2p and we want to distribute it, so we just send the appropriate info bundle
 * to the server.
 */
public class AlarmDistributor extends AbstractAlarmRequestSender implements ResponseListener{

    private final static String TAG = "###AlarmDistributor";

    public AlarmDistributor(Context ctx) {
        super(ctx);
        setResponseListener(this);
    }

    @Override
    public void onResponseReceive(JSONObject json) {
        if(json == null)
            return;
        if(json.has("error")) {
            Log.d(TAG, "couldn't trigger alarm");
        }
        if(json.has("code")){
            try {
                if(!json.get("code").equals("200"))
                    Log.d(TAG, "couldn't trigger alarm");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else {
            Log.d(TAG, "couldn't trigger alarm");
        }
    }

}
