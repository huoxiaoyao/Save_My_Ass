package ch.ethz.inf.vs.a4.savemyass.Centralized;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;

import com.firebase.client.AuthData;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.LocationCallback;

import java.util.LinkedList;
import java.util.List;

import ch.ethz.inf.vs.a4.savemyass.HelperMapCombiner;
import ch.ethz.inf.vs.a4.savemyass.Structure.AlarmCancelReceiver;
import ch.ethz.inf.vs.a4.savemyass.Structure.HelperInfoBundle;
import ch.ethz.inf.vs.a4.savemyass.Structure.HelperOrPinLocationUpdate;
import ch.ethz.inf.vs.a4.savemyass.Structure.PINInfoBundle;

/**
 * Created by jan on 12.12.15.
 *
 * updates the location of the helper in the firebase
 */
public class OnGoingAlarmHelper implements HelperOrPinLocationUpdate, ChildEventListener {

    private static final String TAG = "###OnGoingAlarmHelper";
    private final String firebaseUrl;
    private String userID;
    private Context ctx;
    private List<AlarmCancelReceiver> cancelReceivers;
    private Firebase activeRef, pinLocRef;
    private HelperMapCombiner mapCombiner;
    private PINInfoBundle pinInfoBundle;

    public OnGoingAlarmHelper(Context ctx, String firebaseUrl, HelperMapCombiner mapCombiner, PINInfoBundle infoBundle){
        this.ctx = ctx;
        this.firebaseUrl = firebaseUrl;
        this.mapCombiner = mapCombiner;
        this.pinInfoBundle = infoBundle;
        this.userID = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
        this.cancelReceivers = new LinkedList<>();
        Firebase firebaseRef = new Firebase(firebaseUrl);
        activeRef = firebaseRef.getParent().getRef();
        activeRef.addChildEventListener(this);
        pinLocRef = firebaseRef.getParent().child("pin").getRef();
        pinLocRef.addChildEventListener(this);
    }

    @Override
    public void onLocationUpdate(Location loc) {
        Log.d(TAG, "updating helpers location");
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        String firebaseAuthToken = sp.getString(Config.SHARED_PREFS_FIREBASE_AUTH, "");
        Firebase firebaseRef = new Firebase(firebaseUrl);
        firebaseRef.authWithCustomToken(firebaseAuthToken, new MyAuthResultHandler(firebaseRef, loc));
    }

    public void registerOnCancelListener(AlarmCancelReceiver receiver) {
        cancelReceivers.add(receiver);
    }

    @Override
    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
        if(dataSnapshot.getKey().equals("active")) {
            boolean active = (boolean) dataSnapshot.getValue();
            if (!active) {
                activeRef.removeEventListener(this);
                for (AlarmCancelReceiver r : cancelReceivers)
                    r.onCancel();
            }
        }
    }

    @Override
    public void onChildChanged(DataSnapshot dataSnapshot, String s) {
        if(dataSnapshot.getKey().equals("active")) {
            boolean active = (boolean) dataSnapshot.getValue();
            if (!active) {
                activeRef.removeEventListener(this);
                for (AlarmCancelReceiver r : cancelReceivers)
                    r.onCancel();
            }
        }
        else if(dataSnapshot.getKey().equals("pin")){
            Log.d(TAG, "moved pin!!!");
            GeoFire geoFire = new GeoFire(pinLocRef);
            geoFire.getLocation("location", new LocationCallback() {
                @Override
                public void onLocationResult(String key, GeoLocation location) {
                    Location loc = new Location("");
                    loc.setLatitude(location.latitude);
                    loc.setLongitude(location.longitude);
                    HelperInfoBundle infoBundle = new HelperInfoBundle(pinInfoBundle.userID, loc, System.currentTimeMillis());
                    mapCombiner.add(infoBundle, true);
                }

                @Override
                public void onCancelled(FirebaseError firebaseError) {
                    Log.d(TAG, "firebase cancel");
                }
            });
        }

    }

    @Override
    public void onChildRemoved(DataSnapshot dataSnapshot) {}

    @Override
    public void onChildMoved(DataSnapshot dataSnapshot, String s) {}

    // unused firebase event
    @Override
    public void onCancelled(FirebaseError firebaseError) {
        Log.d(TAG, "firebase cancel");
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
            // for the emulator
            if(loc == null)
                loc = Config.DUMMY_LOC();
            GeoLocation gLoc = new GeoLocation(loc.getLatitude(), loc.getLongitude());
            GeoFire geoFire = new GeoFire(firebaseRef);
            geoFire.setLocation(userID, gLoc);
            Log.d(TAG, "successfully saved location in firebase");
        }

        @Override
        public void onAuthenticationError(FirebaseError firebaseError) {
            Log.d(TAG, "Firebase authentification error");
        }
    }
}
