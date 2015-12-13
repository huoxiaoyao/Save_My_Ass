package ch.ethz.inf.vs.a4.savemyass.Centralized;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;

import ch.ethz.inf.vs.a4.savemyass.Structure.HelperLocationUpdate;

/**
 * Created by jan on 12.12.15.
 *
 * updates the location of the helper in the firebase
 */
public class OnGoingAlarmHelper implements HelperLocationUpdate{

    private static final String TAG = "###OnGoingAlarmHelper";
    private final String firebaseUrl;
    private String userID;
    private Context ctx;


    public OnGoingAlarmHelper(Context ctx, String firebaseUrl){
        this.ctx = ctx;
        this.firebaseUrl = firebaseUrl;
        this.userID = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    @Override
    public void onHelperLocationUpdate(Location loc) {
        Log.d(TAG, "updating helpers location");
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        String firebaseAuthToken = sp.getString(Config.SHARED_PREFS_FIREBASE_AUTH, "");
        Firebase firebaseRef = new Firebase(firebaseUrl);
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
        geoFire.setLocation(userID, gLoc);
        Log.d(TAG, "successfully saved location in firebase");
    }

    @Override
    public void onAuthenticationError(FirebaseError firebaseError) {
        Log.d(TAG, "Firebase authentification error");
    }
}
}
