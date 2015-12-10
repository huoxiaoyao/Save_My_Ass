package ch.ethz.inf.vs.a4.savemyass.UI;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.HashMap;

import ch.ethz.inf.vs.a4.savemyass.Centralized.Config;
import ch.ethz.inf.vs.a4.savemyass.HelpOthers;
import ch.ethz.inf.vs.a4.savemyass.R;
import ch.ethz.inf.vs.a4.savemyass.Structure.AlarmSender;
import ch.ethz.inf.vs.a4.savemyass.Structure.PINInfoBundle;

/**
 * Created by jan on 30.11.15.
 *
 * creates notification and displays it
 */
public class AlarmNotifier implements AlarmSender {

    protected static final String TAG = "###AlarmNotifier";
    private static final int notificationID = 666;
    private final Context ctx;
    // this hash-map stores when the last alarm has been triggered for every user -> so that we don't
    // trigger the same alarm twice (e.g. one from p2p, one from server)
    // todo: store this in shared prefs in case background service gets restarted
    private HashMap<String, Long> lastAlarms;

    public AlarmNotifier(Context ctx){
        this.ctx = ctx;
        this.lastAlarms = new HashMap<>();
    }

    @Override
    public void callForHelp(PINInfoBundle bundle) {
        if(lastAlarms.containsKey(bundle.userID)){
            if(lastAlarms.get(bundle.userID) < System.currentTimeMillis() - Config.MIN_PERIOD_BETWEEN_TWO_ALARMS)
                Log.d(TAG, "not triggering alarm, because already triggered lately");
        }
        else
            lastAlarms.put(bundle.userID, System.currentTimeMillis());
            showNotification(bundle);
    }

    /**
     * Displays notification
     * note: if ID stays unchanged it just updates the notification...
     */
    private void showNotification(PINInfoBundle bundle) {
        String title = "Save My Ass - Alarm!";
        // initialize the notification
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(ctx)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(bundle.toString());
        Intent resultIntent = new Intent(ctx, HelpOthers.class);
        // pass the infobundle as an extra to the activity
        resultIntent.putExtra(Config.INTENT_INFO_BUNDLE, bundle);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        ctx,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        //  allows you to update the notification later on.
        mNotificationManager.notify(notificationID, mBuilder.build());
    }

}
