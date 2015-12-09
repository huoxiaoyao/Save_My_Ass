package ch.ethz.inf.vs.a4.savemyass;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

import ch.ethz.inf.vs.a4.savemyass.Centralized.Config;
import ch.ethz.inf.vs.a4.savemyass.Centralized.GCMReceiver;
import ch.ethz.inf.vs.a4.savemyass.Centralized.GCMSender;
import ch.ethz.inf.vs.a4.savemyass.Centralized.LocationTracker;
import ch.ethz.inf.vs.a4.savemyass.Structure.InfoBundle;
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
    public SimpleAlarmDistributor alarmDistributor, uiDistributor;

    private final IBinder binder = new LocalBinder();
    private List<ServiceDestroyReceiver> serviceDestroyReceivers;
    private LocationTracker locationTracker;

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

        //todo: for the peer-to-peer approach: when you need the userID better use shared preferences
        //directly then taking it form here, make sure to also implement the change listener
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        userID = sp.getString(Config.SHARED_PREFS_USER_ID, "");

        if(!sp.getBoolean(Config.SENT_TOKEN_TO_SERVER, false)){
            //stopSelf();
            Log.d(TAG, "userID is not known yet!");
        }

        Log.d(TAG, "service created");

        // create the distributors
        uiDistributor = new SimpleAlarmDistributor();
        alarmDistributor = new SimpleAlarmDistributor();

        // set up the centralized stuff
        locationTracker = new LocationTracker(getApplicationContext());
        // todo: pass the ui Distributor to this receiver! -> really???
        //GCMReceiver gcmReceiver = new GCMReceiver(uiDistributor);
        Intent i = new Intent(getApplicationContext(), GCMReceiver.class);
//        startService(i);

        GCMSender gcmSender = new GCMSender(getApplicationContext(), locationTracker);
        alarmDistributor.register(gcmSender);

        // set up service destroy receivers
        serviceDestroyReceivers = new LinkedList<>();
        serviceDestroyReceivers.add(locationTracker);
    }

    /**
     * creates info bundle and starts alarm
     */
    public void triggerAlarm() {
        InfoBundle info = new InfoBundle(userID, locationTracker.loggedLocation);
        alarmDistributor.distributeToSend(info);
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
