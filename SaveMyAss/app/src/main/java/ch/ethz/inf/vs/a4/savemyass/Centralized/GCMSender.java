package ch.ethz.inf.vs.a4.savemyass.Centralized;

import android.content.Context;

import ch.ethz.inf.vs.a4.savemyass.Structure.AlarmSender;
import ch.ethz.inf.vs.a4.savemyass.Structure.InfoBundle;

/**
 * Created by jan on 01.12.15.
 *
 * sends a request to the app engine server that starts an alarm
 *
 * todo: how exactly should we propagate the alarm ID and stuff back to the activity to be conform with p2p approach?
 */
public class GCMSender implements AlarmSender {

    private final static String TAG = "###GCMSender";

    private Context ctx;
    private String userID;

    public GCMSender(Context ctx, String userID){
        //todo: do I really need the context?
        this.ctx = ctx;
        this.userID = userID;
    }

    @Override
    public void callForHelp(InfoBundle bundle) {
        //todo: send http request according to:
        //https://developers.google.com/cloud-messaging/downstream
    }
}
