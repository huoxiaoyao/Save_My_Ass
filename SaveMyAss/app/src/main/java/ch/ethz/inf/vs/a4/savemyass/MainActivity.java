package ch.ethz.inf.vs.a4.savemyass;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import ch.ethz.inf.vs.a4.savemyass.Centralized.Config;
import ch.ethz.inf.vs.a4.savemyass.Centralized.GCMRegistrationIntentService;

public class MainActivity extends AppCompatActivity {

    protected final String TAG = "###MainActivity";

    private BackgroundService mBoundService;
    private BroadcastReceiver RegistrationBroadcastReceiver;
    private Intent service;
    private boolean isBound;

    private ProgressBar RegistrationProgressBar;
    protected TextView log;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // dummy log for development purposes
        log = (TextView) findViewById(R.id.log);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){
            Toast toast = Toast.makeText(this, "please grant location permission in settings!", Toast.LENGTH_LONG);
            toast.show();
            this.finish();
        }
        else {
            initializeCentralized();
            if(service == null) {
                service = new Intent(getApplicationContext(), BackgroundService.class);
                startService(service);
                Log.d(TAG, "BackgroundService has been started");
            }
        }

        // alarm trigger button
        Button alarm = (Button) findViewById(R.id.triggerAlarm);
        alarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Intent test = new Intent(Config.INTENT_START_ALARM);
                //test.putExtra("test", "TEST!!!");
                //LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(test);
                mBoundService.triggerAlarm();
            }
        });
    }

    /**
     * does the initialization that is necessary for the centralized approach
     */
    private void initializeCentralized() {
        // if we didn't already sent the user-id to the server, we do it now!
        // note SENT_TOKEN_TO_SERVER set to true also guarantees that there is a user id in the
        // shared prefs
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.edit().putBoolean(Config.SENT_TOKEN_TO_SERVER, false).apply();

        // progress bar that is used for the registration for the centralized approach
        RegistrationProgressBar = (ProgressBar) findViewById(R.id.registrationProgressBar);
        if(!sharedPreferences.getBoolean(Config.SENT_TOKEN_TO_SERVER, false)){
            Intent i = new Intent(this, GCMRegistrationIntentService.class);
            startService(i);
            Log.d(TAG, "GCMRegistrationIntentService has been started");
            // broadcast receiver that gets event as soon as server responded...
            RegistrationBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    RegistrationProgressBar.setVisibility(ProgressBar.GONE);
                    SharedPreferences sp =  PreferenceManager.getDefaultSharedPreferences(context);
                    boolean sentToken = sp.getBoolean(Config.SENT_TOKEN_TO_SERVER, false);
                    log.setText(log.getText()+"\n- token begins with: "+sp.getString(Config.SHARED_PREFS_TOKEN, "").substring(0,10));
                    if (sentToken) {
                        log.setText(log.getText()+"\n- token sent! centralized version is up and running!");
                    } else {
                        log.setText(log.getText()+"\n- error while sending token, NOTIFY THE USER " +
                                "SOMEHOW, he should: close app, make sure internet is enabled and retry");
                    }
                }
            };
        }
        else{
            log.setText(log.getText()+"\n- centralized version is already up and running!");
            RegistrationProgressBar.setVisibility(ProgressBar.GONE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        doBindService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(RegistrationBroadcastReceiver,
                new IntentFilter(Config.REGISTRATION_COMPLETE_BROADCAST));
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(RegistrationBroadcastReceiver);
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        doUnbindService();
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mBoundService = ((BackgroundService.LocalBinder)service).getService();
        }

    public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mBoundService = null;
        }
    };

    protected void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        bindService(service, mConnection, Context.BIND_AUTO_CREATE);
        isBound = true;
    }

    protected void doUnbindService() {
        if (isBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            isBound = false;
        }
    }
}
