package ch.ethz.inf.vs.a4.savemyass;

import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import ch.ethz.inf.vs.a4.savemyass.Centralized.AlarmRequestSender;
import ch.ethz.inf.vs.a4.savemyass.Centralized.Config;
import ch.ethz.inf.vs.a4.savemyass.Structure.AlarmCancelReceiver;
import ch.ethz.inf.vs.a4.savemyass.Structure.HelperMapUpdateReceiver;
import ch.ethz.inf.vs.a4.savemyass.Structure.PINInfoBundle;
import ch.ethz.inf.vs.a4.savemyass.Structure.SimpleAlarmDistributor;

public class HelpRequest extends AppCompatActivity implements OnMapReadyCallback, LocationListener, GoogleApiClient.ConnectionCallbacks,
     GoogleApiClient.OnConnectionFailedListener, HelperMapUpdateReceiver {

    private GoogleMap mMap;
    private static final String TAG = "###HelpRequestActivity";
    private HelperMapCombiner mapCombiner;
    private SimpleAlarmDistributor alarmSender;

    // entry point for Google Play services (used for getting the location)
    protected GoogleApiClient mGoogleApiClient;
    private List<AlarmCancelReceiver> alarmCancelReceivers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_request_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // build Google API client to get the last known location
        buildGoogleApiClient();
        mGoogleApiClient.connect();

        // create the map combiner
        mapCombiner = new HelperMapCombiner();

        // list of cancel receivers
        alarmCancelReceivers = new LinkedList<>();

        // set up the alarm senders
        alarmSender = new SimpleAlarmDistributor();
        alarmSender.register(new AlarmRequestSender(getApplicationContext(), mapCombiner, this));

/*        Button cancel = (Button) findViewById(R.id.cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelAlarm();
            }
        });*/
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
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

    @Override
    protected void onDestroy() {
        mGoogleApiClient.disconnect();
        super.onDestroy();
    }

    /**
     * gets called as soon as the info bundle is ready -> location is available
     */
    private void onInfoBundleReady(PINInfoBundle infoBundle){
        // distribute the alarm to the registered senders
        alarmSender.distributeToSend(infoBundle);
    }

    public void registerOnCancelReceiver(AlarmCancelReceiver receiver){
        alarmCancelReceivers.add(receiver);

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
        //for(Location l : newMap.values())
            //log.setText(log.getText()+"\n  - "+l.toString());
    }

    /**
     *  call this to cancel the alarm
     */
    public void cancelAlarm(){
        for(AlarmCancelReceiver r : alarmCancelReceivers)
            r.onCancel();
        mGoogleApiClient.disconnect();
    }
}
