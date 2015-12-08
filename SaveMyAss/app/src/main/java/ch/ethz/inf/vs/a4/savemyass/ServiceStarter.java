package ch.ethz.inf.vs.a4.savemyass;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/**
 * Created by jan on 08.12.15.
 *
 * starts the background service at system boot
 */

public class ServiceStarter extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Toast toast = Toast.makeText(context, "ServiceStarter now starts the BackgroundService", Toast.LENGTH_LONG);
        toast.show();
        Intent i = new Intent(context, BackgroundService.class);
        context.startService(i);
    }
}
