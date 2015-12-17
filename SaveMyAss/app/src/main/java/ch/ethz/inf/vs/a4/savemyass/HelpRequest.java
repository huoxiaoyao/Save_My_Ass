package ch.ethz.inf.vs.a4.savemyass;

import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import ch.ethz.inf.vs.a4.savemyass.Centralized.AlarmRequestSender;
import ch.ethz.inf.vs.a4.savemyass.Centralized.Config;
import ch.ethz.inf.vs.a4.savemyass.P2p.P2PMaster;
import ch.ethz.inf.vs.a4.savemyass.Structure.AlarmCancelReceiver;
import ch.ethz.inf.vs.a4.savemyass.Structure.HelperMapUpdateReceiver;
import ch.ethz.inf.vs.a4.savemyass.Structure.HelperOrPinLocationUpdate;
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
    // hash map of google map markers
    private HashMap<String, Marker> markers;
    private List<HelperOrPinLocationUpdate> locationUpdates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
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

        // the list of HelperLocationUpdate implementation that will get called if the location changes
        locationUpdates = new LinkedList<>();

        // set up the alarm senders
        alarmSender = new SimpleAlarmDistributor();
        alarmSender.register(new AlarmRequestSender(getApplicationContext(), mapCombiner, this));

        //if we use p2p, also register p2p alarmsender
        if(P2PMaster.lastInstance != null) {
            alarmSender.register(P2PMaster.lastInstance);
        }


        markers = new HashMap<>();
        Button cancel = (Button) findViewById(R.id.cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelAlarm();
            }
        });
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
        mMap.setMyLocationEnabled(true);
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
        // if the user just swipes away the activity we want to cancel the alarm
        cancelAlarm();
        super.onDestroy();
    }

    /**
     * gets called as soon as the info bundle is ready -> location is available
     */
    private void onInfoBundleReady(PINInfoBundle infoBundle){
        // distribute the alarm to the registered senders
        alarmSender.distributeToSend(infoBundle);
        LatLng loc = new LatLng(infoBundle.loc.getLatitude(), infoBundle.loc.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, (float) 14.5));
    }

    public void registerOnCancelReceiver(AlarmCancelReceiver receiver){
        alarmCancelReceivers.add(receiver);
    }

    public void regsiterOnPINLocationChangeReceiver(HelperOrPinLocationUpdate updateReceiver){
        locationUpdates.add(updateReceiver);
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
        for (HelperOrPinLocationUpdate l : locationUpdates)
            l.onLocationUpdate(location);
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
        // get the new hash-map
        HashMap<String, Location> newMap = mapCombiner.getMap();
        if(mMap != null){
            for(String key : newMap.keySet()){
                Location loc = newMap.get(key);
                LatLng newPos = new LatLng(loc.getLatitude(), loc.getLongitude());
                // change the position of the markers if necessary or add them
                if(markers.get(key) == null)
                    markers.put(key, mMap.addMarker(new MarkerOptions().position(newPos).title(getString(R.string.helper_map_info))));
                else
                    markers.get(key).setPosition(newPos);
            }
        }
        else{
            Log.d(TAG, "map isn't ready yet!");
        }
    }

    /**
     *  call this to cancel the alarm
     */
    public void cancelAlarm(){
        for(AlarmCancelReceiver r : alarmCancelReceivers)
            r.onCancel();
        mGoogleApiClient.disconnect();
        finish();
    }
}
