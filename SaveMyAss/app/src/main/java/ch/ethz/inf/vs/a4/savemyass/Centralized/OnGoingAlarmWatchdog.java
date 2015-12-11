package ch.ethz.inf.vs.a4.savemyass.Centralized;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import ch.ethz.inf.vs.a4.savemyass.BackgroundService;
import ch.ethz.inf.vs.a4.savemyass.Structure.PINInfoBundle;

/**
 * Created by jan on 09.12.15.
 *
 * The service for a potential helper.
 *
 * - notifies the user if necessary
 * - enlarges the radius if nobody replied within an amount of time etc.
 *
 */
public class OnGoingAlarmWatchdog extends OnGoingAlarm {

    private BackgroundService bks;
    public OnGoingAlarmWatchdog(Context ctx, String firebaseURL, PINInfoBundle pinInfoBundle, BackgroundService bks){
        super(ctx, firebaseURL, pinInfoBundle);
        this.bks = bks;
    }

    @Override
    void onGeoFireRefReady() {
        //todo: implement logic
        startAlarm();
    }

    // sends an intent with the Info Bundle to the background service
    private void startAlarm() {
        Intent i = new Intent(Config.LOCAL_BROADCAST_START_ALARM);
        i.putExtra(Config.INTENT_INFO_BUNDLE, pinInfoBundle);
        LocalBroadcastManager.getInstance(ctx.getApplicationContext()).sendBroadcast(i);
    }
}
