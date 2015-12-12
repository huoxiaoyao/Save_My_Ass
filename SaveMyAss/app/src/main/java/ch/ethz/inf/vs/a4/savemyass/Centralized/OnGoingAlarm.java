package ch.ethz.inf.vs.a4.savemyass.Centralized;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;

import ch.ethz.inf.vs.a4.savemyass.Structure.PINInfoBundle;

/**
 * Created by jan on 09.12.15.
 *
 * Tracks the current state of the alarm
 */
public abstract class OnGoingAlarm {

    protected static final String TAG = "###OnGoingAlarmService";

    protected Context ctx;
    protected GeoFire geoFire;
    protected GeoLocation pinLocation;
    protected PINInfoBundle pinInfoBundle;
    protected String firebaseURL;

    public OnGoingAlarm(Context ctx, String firebaseURL, PINInfoBundle pinInfoBundle){
        this.ctx = ctx;
        this.pinInfoBundle = pinInfoBundle;
        this.firebaseURL = firebaseURL;
        if(pinInfoBundle == null)
            return;
        pinLocation = new GeoLocation(pinInfoBundle.loc.getLatitude(), pinInfoBundle.loc.getLongitude());
        // get the authentication token from shared preferences
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        String firebaseAuthToken = sp.getString(Config.SHARED_PREFS_FIREBASE_AUTH, "");
        // create firebase reference
        Firebase firebaseRef = new Firebase(firebaseURL);
        // authenticate and create geoFire reference
        firebaseRef.authWithCustomToken(firebaseAuthToken, new MyAuthResultHandler(firebaseRef));
    }

    /**
     * Custom Authentication handler that saves stuff in firebase on success using geofire
     */
    private class MyAuthResultHandler implements Firebase.AuthResultHandler{
        private Firebase firebaseRef;

        public MyAuthResultHandler(Firebase firebaseRef){
            this.firebaseRef = firebaseRef;
        }

        @Override
        public void onAuthenticated(AuthData authData) {
            OnGoingAlarm.this.geoFire = new GeoFire(firebaseRef);
            Log.d(TAG, "successfully created geo fire reference");
            onGeoFireRefReady();
        }

        @Override
        public void onAuthenticationError(FirebaseError firebaseError) {
            Log.d(TAG, "Firebase authentification error");

        }
    }

    abstract void onGeoFireRefReady();
}
