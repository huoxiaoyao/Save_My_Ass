package ch.ethz.inf.vs.a4.savemyass;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

import ch.ethz.inf.vs.a4.savemyass.Centralized.Config;
import ch.ethz.inf.vs.a4.savemyass.Centralized.GCMBackendManager;
import ch.ethz.inf.vs.a4.savemyass.Centralized.GCMSender;
import ch.ethz.inf.vs.a4.savemyass.Centralized.LocationTracker;
import ch.ethz.inf.vs.a4.savemyass.Structure.PINInfoBundle;
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

    private final IBinder binder = new LocalBinder();
    private List<ServiceDestroyReceiver> serviceDestroyReceivers;
    private LocationTracker locationTracker;
    private GCMBackendManager gcmBackendManager;
    public HelperMapCombiner mapCombiner;

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
        locationTracker = new LocationTracker(getApplicationContext());

        GCMSender gcmSender = new GCMSender(getApplicationContext(), locationTracker);
        alarmDistributor.register(gcmSender);

        // set up service destroy receivers
        serviceDestroyReceivers = new LinkedList<>();
        serviceDestroyReceivers.add(locationTracker);

        // this handles the receiving of broadcasts from the gcm backend
        gcmBackendManager = new GCMBackendManager(this, getApplicationContext(), uiDistributor);

        // the thing that will combine the helper maps...
        //TODO: @whoeverdoes the UI: register to this to get updates of the locations of people nearby
        mapCombiner = new HelperMapCombiner();
    }

    /**
     * creates info bundle and starts alarm
     */
    //TODO: @whoeverdoes the UI and alarm trigger logic: call this method to trigger alarm
    public void triggerAlarm() {
        String androidID = Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String message = sp.getString(Config.SHARED_PREFS_USER_MESSAGE, "");
        PINInfoBundle info = new PINInfoBundle(androidID, locationTracker.loggedLocation, message);
        alarmDistributor.distributeToSend(info);
        Intent i = new Intent(getApplicationContext(), HelpRequest.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        gcmBackendManager.registerBroadcastReceivers(intent);
        //todo: the service does no longer get restarted after activity is gone!
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
