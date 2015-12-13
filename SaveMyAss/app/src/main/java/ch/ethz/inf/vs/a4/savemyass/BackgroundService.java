package ch.ethz.inf.vs.a4.savemyass;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.VolumeProvider;
import android.media.session.MediaSession;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;

import java.util.LinkedList;
import java.util.List;

import ch.ethz.inf.vs.a4.savemyass.Centralized.Config;
import ch.ethz.inf.vs.a4.savemyass.Centralized.GCMReceiver;
import ch.ethz.inf.vs.a4.savemyass.Centralized.GCMSender;
import ch.ethz.inf.vs.a4.savemyass.Centralized.LocationTracker;
import ch.ethz.inf.vs.a4.savemyass.Structure.InfoBundle;
import ch.ethz.inf.vs.a4.savemyass.Structure.ServiceDestroyReceiver;
import ch.ethz.inf.vs.a4.savemyass.Structure.SimpleAlarmDistributor;
import ch.ethz.inf.vs.a4.savemyass.UI.ButtonCombination;
import ch.ethz.inf.vs.a4.savemyass.UI.MediaButtonObserver;

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

    private MediaSession buttonEventsMediaSession;
    public ButtonCombination buttonCombination;
    private MediaButtonObserver observer;

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
    @TargetApi(21)
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

        // Button combination object
        buttonCombination = new ButtonCombination(new Runnable() {
            @Override
            public void run() {
                triggerAlarm();
            }
        });

        // listen for button events

        observer = new MediaButtonObserver(new Handler(), getApplicationContext(), new MediaButtonObserver.OnVolumeChangeListener() {
            @Override
            public void onVolumeChange(boolean up) {
                if(up) buttonCombination.onKey(null, KeyEvent.ACTION_UP, null);
                else buttonCombination.onKey(null, KeyEvent.ACTION_DOWN, null);
            }
        });
        this.getApplicationContext().getContentResolver().registerContentObserver(
                android.provider.Settings.System.CONTENT_URI, true,
                observer );

        buttonEventsMediaSession = new MediaSession(getApplicationContext(), "Button event session");
        VolumeProvider vp = new VolumeProvider(VolumeProvider.VOLUME_CONTROL_RELATIVE, 10, 5) {
            @Override
            public void onAdjustVolume(int direction) {
                Log.d(TAG, "volume change");
                switch (direction) {
                    case AudioManager.ADJUST_LOWER:
                        buttonCombination.onKey(null, KeyEvent.ACTION_DOWN, null);
                        break;
                    case AudioManager.ADJUST_RAISE:
                        buttonCombination.onKey(null, KeyEvent.ACTION_UP, null);
                        break;
                }
            }

            @Override
            public void onSetVolumeTo(int volume) {
                Log.d(TAG, "volume set");
                super.onSetVolumeTo(volume);
            }
        };

        buttonEventsMediaSession.setPlaybackToRemote(vp);
        buttonEventsMediaSession.setCallback(new MediaSession.Callback() {
            @Override
            public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
                Log.d(TAG, "volume change");
                int keyCode = ((KeyEvent)mediaButtonIntent.getSerializableExtra(Intent.EXTRA_KEY_EVENT)).getKeyCode();
                if(!buttonCombination.onKey(null, keyCode, null)){
                    return super.onMediaButtonEvent(mediaButtonIntent);
                }
                return true;
            }
        });
        buttonEventsMediaSession.setActive(true);
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
