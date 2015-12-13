package ch.ethz.inf.vs.a4.savemyass.Centralized;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.firebase.client.FirebaseError;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;

import ch.ethz.inf.vs.a4.savemyass.BackgroundService;
import ch.ethz.inf.vs.a4.savemyass.Structure.HelperInfoBundle;
import ch.ethz.inf.vs.a4.savemyass.Structure.PINInfoBundle;

/**
 * Created by jan on 09.12.15.
 *
 * The service for the person in need of help
 *
 * - Gets updates from firebase and displays them.
 */
public class OnGoingAlarmPIN extends OnGoingAlarm {

    protected static final String TAG = "###OnGoingASPIN";

    private BackgroundService bks;

    public OnGoingAlarmPIN(Context ctx, String firebaseURL, PINInfoBundle pinInfoBundle, BackgroundService bks) {
        super(ctx, firebaseURL, pinInfoBundle);
        this.bks = bks;
    }

    @Override
    void onGeoFireRefReady() {
        GeoQuery geoQuery = geoFire.queryAtLocation(pinLocation, Config.ALARM_DISTANCE_THRESHOLD);
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                Log.d(TAG, "enter - key: " + key + " loc: " + location.latitude + "," + location.longitude);
                Location loc = new Location("");
                loc.setLatitude(location.latitude);
                loc.setLongitude(location.longitude);
                HelperInfoBundle infoBundle = new HelperInfoBundle(key, loc, System.currentTimeMillis());
                bks.mapCombiner.add(infoBundle, true);
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
}