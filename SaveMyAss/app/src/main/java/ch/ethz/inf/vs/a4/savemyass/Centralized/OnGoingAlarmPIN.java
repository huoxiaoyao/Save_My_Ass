package ch.ethz.inf.vs.a4.savemyass.Centralized;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;

import java.util.HashMap;

import ch.ethz.inf.vs.a4.savemyass.HelpRequest;
import ch.ethz.inf.vs.a4.savemyass.HelperMapCombiner;
import ch.ethz.inf.vs.a4.savemyass.Structure.AlarmCancelReceiver;
import ch.ethz.inf.vs.a4.savemyass.Structure.HelperInfoBundle;
import ch.ethz.inf.vs.a4.savemyass.Structure.HelperOrPinLocationUpdate;
import ch.ethz.inf.vs.a4.savemyass.Structure.PINInfoBundle;

/**
 * Created by jan on 09.12.15.
 *
 * The service for the person in need of help
 *
 * - Gets updates from firebase and displays them.
 */
public class OnGoingAlarmPIN extends OnGoingAlarm implements AlarmCancelReceiver, HelperOrPinLocationUpdate {

    protected static final String TAG = "###OnGoingASPIN";

    private HelperMapCombiner mapCombiner;

    public OnGoingAlarmPIN(Context ctx, String firebaseURL, PINInfoBundle pinInfoBundle, HelperMapCombiner mapCombiner, HelpRequest requestActivity) {
        super(ctx, firebaseURL, pinInfoBundle);
        this.mapCombiner = mapCombiner;
        requestActivity.registerOnCancelReceiver(this);
        requestActivity.regsiterOnPINLocationChangeReceiver(this);
    }

    @Override
    void onGeoFireRefReady() {
        GeoQuery geoQuery = geoFire.queryAtLocation(pinLocation, 500);
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                Log.d(TAG, "enter - key: " + key + " loc: " + location.latitude + "," + location.longitude);
                Location loc = new Location("");
                loc.setLatitude(location.latitude);
                loc.setLongitude(location.longitude);
                HelperInfoBundle infoBundle = new HelperInfoBundle(key, loc, System.currentTimeMillis());
                mapCombiner.add(infoBundle, true);
            }

            @Override
            public void onKeyExited(String key) {
                // should never happen...
                Log.d(TAG, "exit - key: " + key);
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                Log.d(TAG, "move - key: " + key + " loc: " + location.latitude + "," + location.longitude);
                Location loc = new Location("");
                loc.setLatitude(location.latitude);
                loc.setLongitude(location.longitude);
                HelperInfoBundle infoBundle = new HelperInfoBundle(key, loc, System.currentTimeMillis());
                mapCombiner.add(infoBundle, true);
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

    @Override
    public void onCancel() {
        Firebase firebaseRef = new Firebase(firebaseURL);
        HashMap<String, Object> map = new HashMap<>();
        map.put("active", false);
        firebaseRef.getParent().updateChildren(map);
    }

    @Override
    public void onLocationUpdate(Location loc) {
        Firebase firebaseRef = new Firebase(firebaseURL);
        GeoFire geoFire = new GeoFire(firebaseRef.getParent().child("pin/location"));
        GeoLocation geoLoc = new GeoLocation(loc.getLatitude(), loc.getLongitude());
        geoFire.setLocation("location", geoLoc);
    }
}
