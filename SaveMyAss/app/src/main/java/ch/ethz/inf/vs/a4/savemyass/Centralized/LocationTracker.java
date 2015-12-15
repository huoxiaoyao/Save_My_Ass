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
import android.provider.Settings;
import android.util.Log;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
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
        GoogleApiClient.OnConnectionFailedListener, ServiceDestroyReceiver{


    private Context ctx;
    private ConnectivityManager cm;

    // the ID of the user
    private String userID;
    private String salt;

    // entry point for Google Play services (used for getting the location)
    protected GoogleApiClient mGoogleApiClient;

    // last location sent to the firebase
    protected Location lastSentLocation;
    public Location loggedLocation;

    private static final String TAG = "###LocationTracker";

    // constructors
    public LocationTracker(Context ctx){
        this.ctx = ctx;

        // get userID
        this.userID = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);

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

        Log.d(TAG, "updating location in firebase");
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        String firebaseAuthToken = sp.getString(Config.SHARED_PREFS_FIREBASE_AUTH, "");
        salt = sp.getString(Config.SHARED_PREFS_SALT, "");
        Firebase firebaseRef = new Firebase(Config.FIREBASE_LOCATION_TRACKING);
        firebaseRef.authWithCustomToken(firebaseAuthToken, new MyAuthResultHandler(firebaseRef, loc));
    }

    /**
     * Custom Authentication handler that saves stuff in firebase on success using geofire
     */
    private class MyAuthResultHandler implements Firebase.AuthResultHandler{
        private Firebase firebaseRef;
        private Location loc;

        public MyAuthResultHandler(Firebase firebaseRef, Location loc){
            this.firebaseRef = firebaseRef;
            this.loc = loc;
        }

        @Override
        public void onAuthenticated(AuthData authData) {
            GeoLocation gLoc = new GeoLocation(loc.getLatitude(), loc.getLongitude());
            GeoFire geoFire = new GeoFire(firebaseRef);
            geoFire.setLocation(userID+salt, gLoc);
            lastSentLocation = loc;
            Log.d(TAG, "successfully saved location in firebase");
        }

        @Override
        public void onAuthenticationError(FirebaseError firebaseError) {
            Log.d(TAG, "Firebase authentification error");
        }
    }

    /**
     * Checks if the location is far enough away from the last one sent, and sends it in that case
     */
    private void checkLocationAndUpdate(Location newLocation) {
        Log.d(TAG, "checking if updating location in firebase is needed");
        if (newLocation != null) {
            loggedLocation = newLocation;
            if (lastSentLocation == null) {
                checkInternetAndSend(loggedLocation);
            }
            else if (loggedLocation.distanceTo(lastSentLocation) > Config.LOCATION_TRACKER_SEND_DISTANCE_THRESHOLD) {
                //send current position
                checkInternetAndSend(loggedLocation);
            }
        }
        else{
            checkInternetAndSend(Config.DUMMY_LOC());
        }
    }

    /**
     * checks if internet is available and either sends location or registers broadcast receiver
     */
    private void checkInternetAndSend(Location loc){
        if(hasInternet()) {
            sendLocation(loc);
        }
        else {
            Log.d(TAG, "currently no internet connection, registering connectivity_change receiver");
            //register broadcast receiver and do this when internet is available again...
            IntentFilter filter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
            MyBroadcastReceiver myReceiver = new MyBroadcastReceiver(loc);
            ctx.registerReceiver(myReceiver, filter);
        }
    }

    /**
     * because there's stuff that needs to be cleaned up when the service gets destroyed...
     */
    @Override
    public void onServiceDestroy() {
        Log.d(TAG, "stopping location tracking");
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    /**
     * Broadcast receiver for the internet connectivity event...
     */
    public class MyBroadcastReceiver extends BroadcastReceiver {
        private Location loc;
        public MyBroadcastReceiver(Location loc) {
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
