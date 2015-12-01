package ch.ethz.inf.vs.a4.savemyass.UI;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import ch.ethz.inf.vs.a4.savemyass.MainActivity;
import ch.ethz.inf.vs.a4.savemyass.R;
import ch.ethz.inf.vs.a4.savemyass.Structure.AlarmSender;
import ch.ethz.inf.vs.a4.savemyass.Structure.InfoBundle;

/**
 * Created by jan on 30.11.15.
 *
 * creates notification and displays it
 */
public class NotificationCreator implements AlarmSender {

    private static final int notificationID = 666;
    private final Context ctx;

    NotificationCreator(Context ctx){
        this.ctx = ctx;
    }

    @Override
    public void callForHelp(InfoBundle bundle) {
        //dummy implementation
        showNotification("SaveMyAss", bundle.toString());
    }

    /**
     * Displays notification
     * note: if ID stays unchanged it just updates the notification...
     */
    private void showNotification(String title, String text) {
        // initialize the notification
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(ctx)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(text);
        //todo: change this to active alarm activity ones that implemented
        Intent resultIntent = new Intent(ctx, MainActivity.class);
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
