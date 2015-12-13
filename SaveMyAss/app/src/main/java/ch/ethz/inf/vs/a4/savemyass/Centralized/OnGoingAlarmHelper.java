package ch.ethz.inf.vs.a4.savemyass.Centralized;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;

import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;

import java.util.LinkedList;
import java.util.List;

import ch.ethz.inf.vs.a4.savemyass.Structure.AlarmCancelReceiver;
import ch.ethz.inf.vs.a4.savemyass.Structure.HelperLocationUpdate;

/**
 * Created by jan on 12.12.15.
 *
 * updates the location of the helper in the firebase
 */
public class OnGoingAlarmHelper implements HelperLocationUpdate, ValueEventListener{

    private static final String TAG = "###OnGoingAlarmHelper";
    private final String firebaseUrl;
    private String userID;
    private Context ctx;
    private List<AlarmCancelReceiver> cancelReceivers;
    private Firebase activeRef;


    public OnGoingAlarmHelper(Context ctx, String firebaseUrl){
        this.ctx = ctx;
        this.firebaseUrl = firebaseUrl;
        this.userID = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
        this.cancelReceivers = new LinkedList<>();
        Firebase firebaseRef = new Firebase(firebaseUrl);
        activeRef = firebaseRef.getParent().child("active").getRef();
        activeRef.addValueEventListener(this);
    }

    @Override
    public void onHelperLocationUpdate(Location loc) {
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
    public void onDataChange(DataSnapshot dataSnapshot) {
        boolean active = (boolean) dataSnapshot.getValue();
        if(!active) {
            activeRef.removeEventListener(this);
            for(AlarmCancelReceiver r : cancelReceivers)
                r.onCancel();
        }

    }

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
