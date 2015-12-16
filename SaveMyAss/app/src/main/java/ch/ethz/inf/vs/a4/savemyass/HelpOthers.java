package ch.ethz.inf.vs.a4.savemyass;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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

import java.util.LinkedList;
import java.util.List;

import ch.ethz.inf.vs.a4.savemyass.Centralized.Config;
import ch.ethz.inf.vs.a4.savemyass.Centralized.OnGoingAlarmHelper;
import ch.ethz.inf.vs.a4.savemyass.Structure.AlarmCancelReceiver;
import ch.ethz.inf.vs.a4.savemyass.Structure.HelperMapUpdateReceiver;
import ch.ethz.inf.vs.a4.savemyass.Structure.HelperOrPinLocationUpdate;
import ch.ethz.inf.vs.a4.savemyass.Structure.PINInfoBundle;
import ch.ethz.inf.vs.a4.savemyass.UI.AlarmNotifier;

public class HelpOthers extends AppCompatActivity implements OnMapReadyCallback,
        LocationListener, GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener, AlarmCancelReceiver,
        HelperMapUpdateReceiver{

    private static final String TAG = "###HepOthers";

    private GoogleMap mMap;
    private PINInfoBundle infoBundle;
    private Button accept;
    private List<HelperOrPinLocationUpdate> locationUpdates;
    private boolean accepted;
    private Location lastLocation;
    private HelperMapCombiner mapCombiner;
    private Marker pinMarker;

    // entry point for Google Play services (used for getting the location)
    protected GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_request_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        infoBundle = getIntent().getParcelableExtra(Config.INTENT_INFO_BUNDLE);

        TextView msgView = (TextView) findViewById(R.id.msg);
        String msg = getString(R.string.message_prefix);
        if(infoBundle.message.equals(""))
            msg += getString(R.string.no_personal_message);
        else
            msg += infoBundle.message;

        msgView.setText(msg);

        accept = (Button) findViewById(R.id.accept);
        accept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HelpOthers.this.onAccept();
            }
        });

        Button decline = (Button) findViewById(R.id.decline);
        decline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // the list of HelperLocationUpdate implementation that will get called if the location changes
        locationUpdates = new LinkedList<>();

        // create the map combiner
        mapCombiner = new HelperMapCombiner();
        mapCombiner.register(this);

        // build Google API client to get the last known location
        buildGoogleApiClient();
        mGoogleApiClient.connect();

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String firebaseUrl = sp.getString(Config.INTENT_FIREBASE_ALARM_URL, "");
        OnGoingAlarmHelper alarm = new OnGoingAlarmHelper(getApplicationContext(), firebaseUrl, mapCombiner, infoBundle);
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
        pinMarker = mMap.addMarker(new MarkerOptions().position(pin).title("Person in need of help"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(pin));
        mMap.setMyLocationEnabled(true);
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
            for (HelperOrPinLocationUpdate l : locationUpdates)
                l.onLocationUpdate(location);
        }
        lastLocation = location;
    }

    @Override
    public void onConnected(Bundle bundle) {
        // request location updates
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, createLocationRequest(), this);
        Location loc = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if(accepted) {
            for (HelperOrPinLocationUpdate l : locationUpdates)
                l.onLocationUpdate(loc);
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
        LinearLayout requestLayout = (LinearLayout) findViewById(R.id.requestLayout);
        requestLayout.removeViewAt(requestLayout.getChildCount() - 1);
        accepted = true;
        for (HelperOrPinLocationUpdate l : locationUpdates)
            l.onLocationUpdate(lastLocation);
    }

    @Override
    public void onCancel() {
        mGoogleApiClient.disconnect();
        String text = getString(R.string.alarm_cancelled);
        Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG);
        toast.show();
        String title = getString(R.string.app_name);
        // initialize the notification
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(getApplicationContext())
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(getString(R.string.alarm_cancelled))
                        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                        .setAutoCancel(true);
        Intent resultIntent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        getApplicationContext(),
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //  allows you to update the notification later on.
        mNotificationManager.notify(AlarmNotifier.notificationID, mBuilder.build());
        finish();
    }

    // mapCombiner update: gets called when the map combiner gets an update of the pin location
    @Override
    public void onUpdate() {
        Location newLoc = (Location) mapCombiner.getMap().values().toArray()[0];
        LatLng loc = new LatLng(newLoc.getLatitude(), newLoc.getLongitude());
        pinMarker.setPosition(loc);
    }
}

