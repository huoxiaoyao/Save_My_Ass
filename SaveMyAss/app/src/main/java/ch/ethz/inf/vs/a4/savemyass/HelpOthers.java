package ch.ethz.inf.vs.a4.savemyass;

import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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

import java.util.LinkedList;
import java.util.List;

import ch.ethz.inf.vs.a4.savemyass.Centralized.Config;
import ch.ethz.inf.vs.a4.savemyass.Centralized.OnGoingAlarmHelper;
import ch.ethz.inf.vs.a4.savemyass.Structure.AlarmCancelReceiver;
import ch.ethz.inf.vs.a4.savemyass.Structure.HelperLocationUpdate;
import ch.ethz.inf.vs.a4.savemyass.Structure.PINInfoBundle;

public class HelpOthers extends AppCompatActivity implements OnMapReadyCallback,
        LocationListener, GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener, AlarmCancelReceiver {

    private static final String TAG = "###HepOthers";

    private GoogleMap mMap;
    private TextView log;
    private PINInfoBundle infoBundle;
    private Button accept;
    private List<HelperLocationUpdate> locationUpdates;
    private boolean accepted;
    private Location lastLocation;

    // entry point for Google Play services (used for getting the location)
    protected GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        infoBundle = getIntent().getParcelableExtra(Config.INTENT_INFO_BUNDLE);

        //accept = (Button) findViewById(R.id.accept);
        accept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HelpOthers.this.onAccept();
            }
        });

        // the list of HelperLocationUpdate implementation that will get called if the location changes
        locationUpdates = new LinkedList<>();

        // build Google API client to get the last known location
        buildGoogleApiClient();
        mGoogleApiClient.connect();

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String firebaseUrl = sp.getString(Config.INTENT_FIREBASE_ALARM_URL, "");
        OnGoingAlarmHelper alarm = new OnGoingAlarmHelper(getApplicationContext(), firebaseUrl);
        alarm.registerOnCancelListener(this);
        locationUpdates.add(alarm);
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
        LatLng pin = new LatLng(infoBundle.loc.getLatitude(), infoBundle.loc.getLongitude());
        mMap.addMarker(new MarkerOptions().position(pin).title("Person in need of help"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(pin));
    }

    public void onClick(View v){
        Intent intent = new Intent(this, HelpRequest.class);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        mGoogleApiClient.disconnect();
        super.onDestroy();
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
        if(accepted) {
            for (HelperLocationUpdate l : locationUpdates)
                l.onHelperLocationUpdate(location);
        }
        lastLocation = location;
    }

    @Override
    public void onConnected(Bundle bundle) {
        // request location updates
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, createLocationRequest(), this);
        Location loc = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if(accepted) {
            for (HelperLocationUpdate l : locationUpdates)
                l.onHelperLocationUpdate(loc);
        }
        lastLocation = loc;
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


    private void onAccept(){
        log.setText(log.getText()+"\n- accepted alarm");
        accept.setClickable(false);
        accepted = true;
        for (HelperLocationUpdate l : locationUpdates)
            l.onHelperLocationUpdate(lastLocation);
    }

    @Override
    public void onCancel() {
        mGoogleApiClient.disconnect();
        log.setText(log.getText() + "\n- alarm has been cancelled");
    }
}

