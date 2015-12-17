package ch.ethz.inf.vs.a4.savemyass.UI;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

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
    public static final int notificationID = 666;
    private final Context ctx;

    public AlarmNotifier(Context ctx){
        this.ctx = ctx;
    }

    @Override
    public void callForHelp(PINInfoBundle bundle) {
        showNotification(bundle);
    }

    /**
     * Displays notification
     * note: if ID stays unchanged it just updates the notification...
     */
    private void showNotification(PINInfoBundle bundle) {
        String title = ctx.getString(R.string.app_name);
        // initialize the notification
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(ctx)
                        .setSmallIcon(R.mipmap.ic_launcher_logo)
                        .setContentTitle(title)
                        .setContentText(ctx.getString(R.string.alarm_triggered_notification))
                        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                        .setAutoCancel(true);
        Intent resultIntent = new Intent(ctx, HelpOthers.class);
        // pass the info-bundle as an extra to the activity
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
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx.getApplicationContext());
        boolean alreadyActive = sp.getBoolean(Config.SHARED_PREFS_ALARM_ACTIVE, false);
        if(!alreadyActive)
            mNotificationManager.notify(notificationID, mBuilder.build());
    }

}
