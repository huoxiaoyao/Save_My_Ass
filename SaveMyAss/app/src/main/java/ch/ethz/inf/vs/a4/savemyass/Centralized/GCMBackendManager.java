package ch.ethz.inf.vs.a4.savemyass.Centralized;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import ch.ethz.inf.vs.a4.savemyass.BackgroundService;
import ch.ethz.inf.vs.a4.savemyass.Structure.PINInfoBundle;
import ch.ethz.inf.vs.a4.savemyass.Structure.ServiceDestroyReceiver;
import ch.ethz.inf.vs.a4.savemyass.Structure.SimpleAlarmDistributor;

/**
 * Created by jan on 10.12.15.
 *
 * Handles the receiving of broadcasts from the gcm backend
 *
 * - broadcasts are sent from GCMSender and GCMReceiver
 */
public class GCMBackendManager implements ServiceDestroyReceiver{
    protected static String TAG = "###GCMBackendManager";
    private Context ctx;
    private SimpleAlarmDistributor uiDistributor;
    private BackgroundService bks;
    private OnGoingAlarmPIN pinAlarm;
    private Intent i;

    public BroadcastReceiver alarmBroadcastReceiver, helperBroadcastReceiver, pinBroadcastReceiver;

    public GCMBackendManager(BackgroundService bks, Context ctx, SimpleAlarmDistributor uiDistributor){
        this.bks = bks;
        this.ctx = ctx;
        this.uiDistributor = uiDistributor;
    }

    public void registerBroadcastReceivers(Intent intent) {
        i = intent;
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(ctx);
        Config.BackgroundServiceStartMode mode = (Config.BackgroundServiceStartMode) intent.getSerializableExtra(Config.MODE);
        // depending on what overall system state we're in we want to handle stuff differently
        if (mode == null){
            mode = Config.BackgroundServiceStartMode.NORMAL;
        }
        switch  (mode){
            case NORMAL:
                alarmBroadcastReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        PINInfoBundle pinInfo = (PINInfoBundle) intent.getSerializableExtra(Config.INTENT_INFO_BUNDLE);
                        Log.d(TAG, "starting alarm! with pinInfo: " + pinInfo.toString());
                        uiDistributor.distributeToSend(pinInfo);
                    }
                };
                lbm.registerReceiver(alarmBroadcastReceiver, new IntentFilter(Config.LOCAL_BROADCAST_START_ALARM));
                break;
            case ALARM_PIN:
                pinBroadcastReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        // InfoBundle data included in the Intent
                        Log.d(TAG, "received firebase url");
                        Log.d(TAG, intent.getStringExtra("test"));
                        PINInfoBundle pinInfo = (PINInfoBundle) intent.getSerializableExtra(Config.INTENT_INFO_BUNDLE);
                        String firebaseUrl = intent.getStringExtra(Config.INTENT_FIREBASE_ALARM_URL);
                        pinAlarm = new OnGoingAlarmPIN(ctx, firebaseUrl, pinInfo, bks);

                    }
                };
                lbm.registerReceiver(pinBroadcastReceiver, new IntentFilter(Config.LOCAL_BROADCAST_PIN_ALARM_INFO));
                break;
            case ALARM_HELPER:
                break;
        }
    }


    @Override
    public void onServiceDestroy() {
        Config.BackgroundServiceStartMode mode = (Config.BackgroundServiceStartMode) i.getSerializableExtra(Config.MODE);
        switch  (mode) {
            case NORMAL:
                LocalBroadcastManager.getInstance(ctx).unregisterReceiver(alarmBroadcastReceiver);
                break;
            case ALARM_PIN:
                LocalBroadcastManager.getInstance(ctx).unregisterReceiver(pinBroadcastReceiver);
                break;
            case ALARM_HELPER:
                break;
        }
    }
}
