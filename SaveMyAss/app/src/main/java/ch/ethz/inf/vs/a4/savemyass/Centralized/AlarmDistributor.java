package ch.ethz.inf.vs.a4.savemyass.Centralized;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

/**
 * Created by jan on 11.12.15.
 *  
 *
 */
public class AlarmDistributor extends AbstractAlarmRequestSender implements ResponseListener{

    private final static String TAG = "###AlarmDistributor";

    public AlarmDistributor(Context ctx) {
        super(ctx);
        setResponseListener(this);
    }

    @Override
    public void onResponseReceive(JSONObject response) {
        if(response == null){
            Log.d(TAG, "server responded with null");
            return;
        }

        //todo: do appropriate error handling here!

        //todo: implement the case where we want to send someone else's alarm further on (got to us by p2p)
    }

}
