package ch.ethz.inf.vs.a4.savemyass;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

/**
 * Created by jan on 14.12.15.
 *
 * - starts the service if not yet started
 */
public class WakefulServiceStarter extends WakefulBroadcastReceiver {

    private static final String TAG = "###WakefulSStarter";

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean serviceRunning = isMyServiceRunning(BackgroundService.class, context);
        Log.d(TAG, "service running? -> "+serviceRunning);
        if(!serviceRunning) {
            Log.d(TAG, "starting service");
            Intent i = new Intent(context, BackgroundService.class);
            context.startService(i);
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass, Context ctx) {
        ActivityManager manager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
