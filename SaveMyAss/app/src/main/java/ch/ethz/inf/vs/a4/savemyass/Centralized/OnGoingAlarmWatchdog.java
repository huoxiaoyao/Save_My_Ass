package ch.ethz.inf.vs.a4.savemyass.Centralized;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import ch.ethz.inf.vs.a4.savemyass.Structure.PINInfoBundle;
import ch.ethz.inf.vs.a4.savemyass.UI.AlarmNotifier;

/**
 * Created by jan on 09.12.15.
 *
 * The service for a potential helper.
 *
 * - notifies the user if necessary
 * - enlarges the radius if nobody replied within an amount of time etc.
 *
 */
public class OnGoingAlarmWatchdog extends OnGoingAlarm implements LocationListener, GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener{

    // entry point for Google Play services (used for getting the location)
    protected GoogleApiClient mGoogleApiClient;

    public Location loggedLocation;
    private int radiusIndex = 0;
    private Timer timer;
    private Handler handler = new Handler(Looper.getMainLooper());
    private int counter = 0;
    private Lock radiusIndexLock = new ReentrantLock();
    private Lock counterLock = new ReentrantLock();
    private boolean alreadyTriggered = false;

    public OnGoingAlarmWatchdog(Context ctx, String firebaseURL, PINInfoBundle pinInfoBundle){
        super(ctx, firebaseURL, pinInfoBundle);
        // build Google API client to get the last known location
        buildGoogleApiClient();
        mGoogleApiClient.connect();
    }

    @Override
    void onGeoFireRefReady() {
        timer = new Timer();
        int delay = 0;
        for(int i=0; i<Config.WATCHDOG_RADIUS.length-1; i++){
            // just that we don't get a stupid index out of bounds exception if we messed up the configurations
            int j;
            if(i>=Config.WATCHDOG_TIMES.length)
                j = Config.WATCHDOG_TIMES.length-1;
            else
                j = i;
            delay += Config.WATCHDOG_TIMES[j]*1000;
            // schedule the CountChecker task
            timer.schedule(new CountChecker(), delay);
        }

        GeoQuery geoQuery = geoFire.queryAtLocation(pinLocation, Config.ALARM_DISTANCE_THRESHOLD);
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                Log.d(TAG, "enter - key: " + key + " loc: " + location.latitude + "," + location.longitude);
                Location newLoc = new Location("");
                newLoc.setLatitude(location.latitude);
                newLoc.setLongitude(location.longitude);
                if (pinInfoBundle.loc.distanceTo(newLoc) / 1000 <= getCurrentRadius()) {
                    increaseCounter();
                    Log.d(TAG, "somebody within the radius actually responded!");
                }
            }

            @Override
            public void onKeyExited(String key) {
                Log.d(TAG, "exit - key: " + key);
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                Log.d(TAG, "move - key: " + key + " loc: " + location.latitude + "," + location.longitude);
            }

            @Override
            public void onGeoQueryReady() {
                Log.d(TAG, "All initial data has been loaded and events have been fired!");
            }

            @Override
            public void onGeoQueryError(FirebaseError error) {
                Log.d(TAG, "There was an error with this query: " + error);
            }
        });
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
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return mLocationRequest;
    }

    /**
     * Helper methods to access and change the current radius and counter from within TimerTask
     */
    protected void increaseCounter(){
        counterLock.lock();
        counter++;
        counterLock.unlock();
    }
    protected int getCounter(){
        counterLock.lock();
        int i = counter;
        counterLock.unlock();
        return i;
    }
    protected void increaseRadius(){
        radiusIndexLock.lock();
        radiusIndex++;
        radiusIndexLock.unlock();
        maybeTrigger(loggedLocation);
    }
    public synchronized double getCurrentRadius(){
        radiusIndexLock.lock();
        int i = radiusIndex;
        radiusIndexLock.unlock();
        return Config.WATCHDOG_RADIUS[i];
    }

    @Override
    public void onConnected(Bundle bundle) {
        // request location updates
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, createLocationRequest(), this);
        loggedLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        maybeTrigger(loggedLocation);
    }

    @Override
    public void onLocationChanged(Location location) {
        loggedLocation = location;
        maybeTrigger(loggedLocation);
    }

    // triggers the alarm if we are close enough
    private synchronized void maybeTrigger(Location loggedLocation) {
        if(alreadyTriggered)
            return;
        // for debugging purposes
        if(loggedLocation == null)
            loggedLocation = Config.DUMMY_LOC();
        if(loggedLocation.distanceTo(pinInfoBundle.loc)/1000 < getCurrentRadius()) {
            Firebase activeRef = new Firebase(firebaseURL);
            activeRef = activeRef.getParent().child("active").getRef();
            activeRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    boolean active = (boolean) dataSnapshot.getValue();
                    if(active) {
                        alreadyTriggered = true;
                        startAlarm();
                        if (timer != null)
                            timer.cancel();
                    }
                    else{
                        mGoogleApiClient.disconnect();
                        if (timer != null)
                            timer.cancel();
                    }
                }

                @Override
                public void onCancelled(FirebaseError firebaseError) {
                    Log.d(TAG, "firebase query failed");
                }
            });
        }
    }

    /**
     * Task that gets executed delayed, enlarges the radius of consideration if nobody accepted the alarm yet
     */
    private class CountChecker extends TimerTask {
        @Override
        public void run() {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    // check if there are already enough helper on their way
                    if (getCounter() < Config.WATCHDOG_RESPOND_COUNT_THRESHOLD) {
                        increaseRadius();
                        Log.d(TAG, "not enough people responded... enlarging the radius to: "+getCurrentRadius());
                    }
                    else {
                        timer.cancel();
                        Log.d(TAG, "enough people responded... not enlarging the radius");
                    }
                }
            });
        }
    }

    // shows the notification and starts alarm
    private void startAlarm() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        sp.edit().putString(Config.INTENT_FIREBASE_ALARM_URL, firebaseURL).apply();
        mGoogleApiClient.disconnect();
        AlarmNotifier ai = new AlarmNotifier(ctx);
        ai.callForHelp(pinInfoBundle);
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

}
