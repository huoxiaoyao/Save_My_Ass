package ch.ethz.inf.vs.a4.savemyass;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    protected final String TAG = "###MainActivity";
    private BackgroundService mBoundService;
    private boolean isBound;
    private Intent service;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){
            Toast toast = Toast.makeText(this, "please grant location permission in settings!", Toast.LENGTH_LONG);
            toast.show();
            this.finish();
        }
        else {
            if(service == null) {
                Log.d(TAG, "creating and starting service");
                service = new Intent(getApplicationContext(), BackgroundService.class);
                startService(service);
            }

        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        doBindService();
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
