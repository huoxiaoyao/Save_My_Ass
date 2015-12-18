package ch.ethz.inf.vs.a4.savemyass;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

import ch.ethz.inf.vs.a4.savemyass.Centralized.AlarmDistributor;
import ch.ethz.inf.vs.a4.savemyass.Centralized.Config;
import ch.ethz.inf.vs.a4.savemyass.Centralized.LocationTracker;
import ch.ethz.inf.vs.a4.savemyass.Structure.ServiceDestroyReceiver;
import ch.ethz.inf.vs.a4.savemyass.Structure.SimpleAlarmDistributor;
import ch.ethz.inf.vs.a4.savemyass.UI.AlarmNotifier;

/**
 * Created by jan on 30.11.15.
 *
 * service that runs in the background
 * note:
 *  - can get restarted (when activity is swiped away)
 *  - other classes that implement the ServiceDestroyReceiver can "register" for getting called when
 *    the service gets killed.
 */
public class BackgroundService extends Service{

    public String TAG = "###BackgroundService";

    public SimpleAlarmDistributor alarmDistributor, uiDistributor;

    private List<ServiceDestroyReceiver> serviceDestroyReceivers;

    @Override
    public void onCreate() {
        super.onCreate();

        // check if gcm set up correctly
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        if(!sp.getBoolean(Config.SENT_TOKEN_TO_SERVER, false)){
            stopSelf();
            Log.d(TAG, "token is not sent to server yet!");
        }

        Log.d(TAG, "service created");

        // create the distributors
        // ui Distributor notifies the UI where an alarm happened -> shows notification and opens
        uiDistributor = new SimpleAlarmDistributor();
        uiDistributor.register(new AlarmNotifier(getApplicationContext()));

        // alarm Distributor distributes a given alarm further on
        alarmDistributor = new SimpleAlarmDistributor();

        // set up the centralized stuff
        LocationTracker locationTracker = new LocationTracker(getApplicationContext());

        AlarmDistributor gcmDistributor = new AlarmDistributor(getApplicationContext());
        alarmDistributor.register(gcmDistributor);

        // set up service destroy receivers
        serviceDestroyReceivers = new LinkedList<>();
        serviceDestroyReceivers.add(locationTracker);

        // register alarm manager to check if service is running and start it in cases it's no yet
        // running (done in WakefulServiceStarter)
        AlarmManager alarmMgr = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(getApplicationContext(), WakefulServiceStarter.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, 0);

        alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                AlarmManager.INTERVAL_FIFTEEN_MINUTES,
                AlarmManager.INTERVAL_FIFTEEN_MINUTES, alarmIntent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // call onServiceDestroy for all the registered implementations of ServiceDestroyReceiver
        for(ServiceDestroyReceiver sdr : serviceDestroyReceivers){
            sdr.onServiceDestroy();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
