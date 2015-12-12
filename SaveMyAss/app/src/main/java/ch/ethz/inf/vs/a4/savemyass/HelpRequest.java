package ch.ethz.inf.vs.a4.savemyass;

import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.HashMap;

import ch.ethz.inf.vs.a4.savemyass.Centralized.AlarmRequestSender;
import ch.ethz.inf.vs.a4.savemyass.Centralized.Config;
import ch.ethz.inf.vs.a4.savemyass.Structure.HelperMapUpdateReceiver;
import ch.ethz.inf.vs.a4.savemyass.Structure.PINInfoBundle;
import ch.ethz.inf.vs.a4.savemyass.Structure.SimpleAlarmDistributor;

public class HelpRequest extends AppCompatActivity implements LocationListener, GoogleApiClient.ConnectionCallbacks,
     GoogleApiClient.OnConnectionFailedListener, HelperMapUpdateReceiver{

    private static final String TAG = "###HelpRequestActivity";

    private TextView log;
    private HelperMapCombiner mapCombiner;
    private SimpleAlarmDistributor alarmSender;

    // entry point for Google Play services (used for getting the location)
    protected GoogleApiClient mGoogleApiClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help_request);
        log = (TextView) findViewById(R.id.help_request_log);
        log.setText(log.getText() + "\n- alarm!");

        // build Google API client to get the last known location
        buildGoogleApiClient();
        mGoogleApiClient.connect();

        // create the map combiner
        mapCombiner = new HelperMapCombiner();


        // set up the alarm senders
        alarmSender = new SimpleAlarmDistributor();
        alarmSender.register(new AlarmRequestSender(getApplicationContext(), mapCombiner));
    }

    @Override
    protected void onPause() {
        mapCombiner.unregsiter(this);
        super.onPause();
    }

    @Override
    protected void onResume() {
        mapCombiner.register(this);
        super.onResume();
    }

    /**
     * gets called as soon as the info bundle is ready -> location is available
     */
    private void onInfoBundleReady(PINInfoBundle infoBundle){
        // distribute the alarm to the registered senders
        alarmSender.distributeToSend(infoBundle);
    }

    // creates location request
    private LocationRequest createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(Config.LOCATION_TRACKER_UPDATE_PERIOD);
        mLocationRequest.setFastestInterval(Config.LOCATION_TRACKER_UPDATE_PERIOD_MIN);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return mLocationRequest;
    }

    // creates google api client
    private synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    /**
     * gets called when the users location changes
     */
    @Override
    public void onLocationChanged(Location location) {
        // todo: change the users location on the map
    }

    @Override
    public void onConnected(Bundle bundle) {
        // request location updates
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, createLocationRequest(), this);
        String userID = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        Location loc = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String msg = sp.getString(Config.SHARED_PREFS_USER_MESSAGE, "");
        PINInfoBundle infoBundle = new PINInfoBundle(userID, loc, msg);
        onInfoBundleReady(infoBundle);
    }

    @Override
    public void onConnectionSuspended(int i) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.d(TAG, "Connection to Google Play services suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "Connection to Google Play services failed");
        mGoogleApiClient.connect();
    }

    /**
     * Gets called when the location of a person on their way changed
     */
    @Override
    public void onUpdate() {
        //todo update the UI
        // read the changes from this object here:
        HashMap<String, Location> newMap = mapCombiner.getMap();
        log.setText("Log:\ngot an update of some helper locations:");
        for(Location l : newMap.values())
            log.setText(log.getText()+"\n  - "+l.toString());
    }
}
