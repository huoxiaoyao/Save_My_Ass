package ch.ethz.inf.vs.a4.savemyass.P2p;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.List;

import ch.ethz.inf.vs.a4.savemyass.Structure.ServiceDestroyReceiver;

/**
 * Created by Fabian_admin on 17.12.2015.
 */
public class OwnLocationDistributor implements LocationListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, ServiceDestroyReceiver {

    private static final String TAG = "##LocationDistributor##";

    private Context context;
    private List<OwnLocationUpdateListener> observers;
    private GoogleApiClient mGoogleApiClient;

    public OwnLocationDistributor(Context context) {
        this.context = context;

        buildGoogleApiClient();
        mGoogleApiClient.connect();
    }

    public void registerListener(OwnLocationUpdateListener listener){
        observers.add(listener);
    }

    public void deregisterListener(OwnLocationUpdateListener listener) {
        observers.remove(listener);
    }

    private synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    private LocationRequest createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(ConfigP2p.LOCATION_TRACKER_UPDATE_PERIOD);
        mLocationRequest.setFastestInterval(ConfigP2p.LOCATION_TRACKER_UPDATE_PERIOD_MIN);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);
        return mLocationRequest;
    }

    @Override
    public void onConnected(Bundle bundle) {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, createLocationRequest(), this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Connection to Google Play services suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        for(OwnLocationUpdateListener o: observers) {
            o.onOwnLocationUpdate(location);
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "Connection to Google Play services failed");
        mGoogleApiClient.connect();
    }

    @Override
    public void onServiceDestroy() {
        Log.d(TAG, "stopping location tracking");
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }
}
