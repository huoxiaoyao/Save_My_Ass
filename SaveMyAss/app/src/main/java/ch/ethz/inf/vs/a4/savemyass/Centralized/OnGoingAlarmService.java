package ch.ethz.inf.vs.a4.savemyass.Centralized;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.geofire.GeoFire;

/**
 * Created by jan on 09.12.15.
 *
 * Tracks the current state of the alarm
 * - notifies the user if necessary
 * - enlarges the radius if nobody replied within an amount of time etc.
 */
// todo: implement this service
public class OnGoingAlarmService extends Service {

    protected static final String TAG = "###OnGoingAlarmService";

    protected GeoFire geoFire;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // get the url from the intent
        String firebaseURL = intent.getStringExtra(Config.INTENT_FIREBASE_ALARM_URL);
        // get the authentication token from shared preferences
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String firebaseAuthToken = sp.getString(Config.SHARED_PREFS_FIREBASE_AUTH, "");
        // create firebase reference
        Firebase firebaseRef = new Firebase(firebaseURL);
        // authenticate and create geoFire reference
        firebaseRef.authWithCustomToken(firebaseAuthToken, new MyAuthResultHandler(firebaseRef));
        return super.onStartCommand(intent, flags, startId);
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
            OnGoingAlarmService.this.geoFire = new GeoFire(firebaseRef);
            Log.d(TAG, "successfully created geo fire reference");
        }

        @Override
        public void onAuthenticationError(FirebaseError firebaseError) {
            Log.d(TAG, "Firebase authentification error");

        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
