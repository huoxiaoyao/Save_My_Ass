package ch.ethz.inf.vs.a4.savemyass.Centralized;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.firebase.client.Firebase;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import ch.ethz.inf.vs.a4.savemyass.Structure.ServiceDestroyReceiver;

/**
 * Created by jan on 01.12.15.
 *
 * tracks the current location and updates in firebase if necessary
 */
public class LocationTracker implements LocationListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, ServiceDestroyReceiver,
        SharedPreferences.OnSharedPreferenceChangeListener{

    private Context ctx;
    private ConnectivityManager cm;

    // the ID of the user
    private String userID;

    // entry point for Google Play services (used for getting the location)
    protected GoogleApiClient mGoogleApiClient;

    // last location sent to the firebase
    protected Location lastSentLocation;
    public Location loggedLocation;

    private static final String TAG = "###LocationTracker";

    // constructor
    public LocationTracker(Context ctx){
        this.ctx = ctx;

        // get userID and register listener for userID change
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);
        this.userID = sharedPreferences.getString(Config.SHARED_PREFS_USER_ID, "");

        // set firebase android context
        Firebase.setAndroidContext(ctx);

        // build Google API client to get the last known location
        buildGoogleApiClient();
        mGoogleApiClient.connect();

        // connectivity manager initialization, used for hasInternet
        cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    private synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(ctx)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    private LocationRequest createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(Config.LOCATION_TRACKER_UPDATE_PERIOD);
        mLocationRequest.setFastestInterval(Config.LOCATION_TRACKER_UPDATE_PERIOD_MIN);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);
        return mLocationRequest;
    }

    /**
     * Returns true if internet is available
     */
    protected boolean hasInternet(){
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }

    /**
     * Saves location in firebase
     */
    private void sendLocation(Location loc){
        Firebase firebaseRef = new Firebase(Config.FIREBASE_LOCATION_TRACKING);
        GeoLocation gLoc = new GeoLocation(loc.getLatitude(), loc.getLongitude());
        GeoFire geoFire = new GeoFire(firebaseRef);
        geoFire.setLocation(userID, gLoc);
        lastSentLocation = loc;
    }

    /**
     * Checks if the location is far enough away from the last one sent, and sends it in that case
     */
    private void checkLocationAndUpdate(Location lastLocation) {
        Log.d(TAG, "checking if updating location in firebase is needed");
        if (lastLocation != null) {
            Log.d(TAG, "loc: "+lastLocation.toString());
            loggedLocation = lastLocation;
            if (lastSentLocation == null) {
                checkInternetAndSend(loggedLocation);
            }
            else if (loggedLocation.distanceTo(lastSentLocation) > Config.LOCATION_TRACKER_SEND_DISTANCE_THRESHOLD) {
                //send current position
                checkInternetAndSend(loggedLocation);
            }
        }
    }

    /**
     * checks if internet is available and either sends location or registers broadcast receiver
     */
    private void checkInternetAndSend(Location loc){
        if(hasInternet()) {
            Log.d(TAG, "updating location in firebase...");
            sendLocation(loc);
        }
        else {
            Log.d(TAG, "currently no internet connection, registering connectivity_change receiver");
            //register broadcast receiver and do this when internet is available again...
            IntentFilter filter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
            MyReceiver myReceiver = new MyReceiver(loc);
            ctx.registerReceiver(myReceiver, filter);
        }
    }

    /**
     * because there's stuff that needs to be cleaned up when the service gets destroyed...
     */
    @Override
    public void onServiceDestroy() {
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    /**
     * updates the user id when it's changed in the preferences for some reason...
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals(Config.SHARED_PREFS_USER_ID))
            userID = sharedPreferences.getString(Config.SHARED_PREFS_USER_ID, "");
    }

    /**
     * Broadcast receiver for the internet connectivity event...
     */
    public class MyReceiver extends BroadcastReceiver {
        private Location loc;
        public MyReceiver(Location loc) {
            this.loc = loc;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            // This method is called when this BroadcastReceiver receives an Intent broadcast.
            if(hasInternet()) {
                Log.d(TAG, "internet available -> sending location now");
                sendLocation(loc);
            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        // request location updates
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, createLocationRequest(), this);
        checkLocationAndUpdate(LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient));
    }

    @Override
    public void onConnectionSuspended(int i) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.d(TAG, "Connection to Google Play services suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        checkLocationAndUpdate(location);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "Connection to Google Play services failed");
        mGoogleApiClient.connect();
    }
}
