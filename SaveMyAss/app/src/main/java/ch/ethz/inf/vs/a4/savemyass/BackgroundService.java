package ch.ethz.inf.vs.a4.savemyass;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

import ch.ethz.inf.vs.a4.savemyass.Centralized.GCMReceiver;
import ch.ethz.inf.vs.a4.savemyass.Centralized.GCMSender;
import ch.ethz.inf.vs.a4.savemyass.Centralized.LocationTracker;
import ch.ethz.inf.vs.a4.savemyass.Structure.ServiceDestroyReceiver;
import ch.ethz.inf.vs.a4.savemyass.Structure.SimpleAlarmDistributor;

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

    public String userID;

    private final IBinder binder = new LocalBinder();

    private List<ServiceDestroyReceiver> serviceDestroyReceivers;

    /**
     * Class for clients to access.  Because we know this service always runs in the same process as
     * its clients, we don't need to deal with IPC but can simply bind or unbind from any activity.
     */
    public class LocalBinder extends Binder {
        public BackgroundService getService() {
            return BackgroundService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // todo: get the userID!!!!
        // -> probably do this in the main activity at first startup and put it in shared prefs
        // this is just a dummy userID that changes every time the service gets created.
        userID = "t"+System.currentTimeMillis();

        Log.d(TAG, "service created");

        // create the distributors
        SimpleAlarmDistributor uiDistributor = new SimpleAlarmDistributor();
        SimpleAlarmDistributor alarmDistributor = new SimpleAlarmDistributor();

        // set up the centralized stuff
        GCMReceiver gcmReceiver = new GCMReceiver(uiDistributor);
        GCMSender gcmSender = new GCMSender(getApplicationContext(), userID);
        serviceDestroyReceivers = new LinkedList<>();
        serviceDestroyReceivers.add(new LocationTracker(getApplicationContext(), userID));
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
        return binder;
    }
}
