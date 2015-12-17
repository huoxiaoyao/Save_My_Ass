package ch.ethz.inf.vs.a4.savemyass.Centralized;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;

import ch.ethz.inf.vs.a4.savemyass.R;

/**
 * Created by jan on 03.12.15.
 *
 * creates the GCM token if necessary, safes it to shared preferences and sends it to the server
 * if everything went right it sets SENT_TOKEN_TO_SERVER in shared prefs to true, otherwise to false
 *
 * start an instance of this service intent whenever the user_id is not set in the shared preferences
 * or SENT_TOKEN_TO_SERVER is false.
 *
 */
public class GCMRegistrationIntentService extends IntentService {

    public String TAG = "###GCMRegistrationIntentService";

    private static final String[] TOPICS = {"global"};

    public GCMRegistrationIntentService(){
        super("GCMRegistrationIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Log.d(TAG, "getting GCM token and sending it to server...");
        boolean newToken = intent.getBooleanExtra(Config.INTENT_NEW_TOKEN, false);
        try {
            String token, salt;
            // check if we already have an token saved in the shared preferences or we explicitly
            // should generate a newToken because the GCMInstanceIDListener called this
            if(!sharedPreferences.contains(Config.SHARED_PREFS_TOKEN) || newToken){
                InstanceID instanceID = InstanceID.getInstance(this);
                // note: R.string.gcm_defaultSenderID is given by the google-services.json file
                token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
                        GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
                Log.d(TAG, "new token:" + token);
                // save the token in the shared preferences
                sharedPreferences.edit().putString(Config.SHARED_PREFS_TOKEN, token).apply();

                // generate salt
                SecureRandom random = new SecureRandom();
                salt = (new BigInteger(130, random)).toString(32);
                Log.d(TAG, "new salt: " + salt);
                // save salt in the shared preferences
                sharedPreferences.edit().putString(Config.SHARED_PREFS_SALT, salt).apply();
            }
            // otherwise sent the token we already have to the server
            else {
                token = sharedPreferences.getString(Config.SHARED_PREFS_TOKEN, "");
                salt = sharedPreferences.getString(Config.SHARED_PREFS_SALT, "");
                Log.d(TAG, "token:" + token);
                Log.d(TAG, "salt:" + salt);
            }

            boolean success = sendRegistrationToServer(token, salt);
            // store boolean whether the generated token has been sent to the server.
            if(success)
                sharedPreferences.edit().putBoolean(Config.SENT_TOKEN_TO_SERVER, true).apply();
            else
                sharedPreferences.edit().putBoolean(Config.SENT_TOKEN_TO_SERVER, false).apply();

            // Subscribe to topic channels
            subscribeTopics(token);
        } catch (Exception e) {
            Log.d(TAG, "Failed to complete token refresh", e);
            // in case something went wrong, we need to save that we didn't sent the token to the server.
            sharedPreferences.edit().putBoolean(Config.SENT_TOKEN_TO_SERVER, false).apply();
        }
        // Notify UI that registration has completed, so the progress indicator can be hidden.
        Intent registrationComplete = new Intent(Config.REGISTRATION_COMPLETE_BROADCAST);
        LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
    }

    /**
     * sends the generate token and some salt to the app engine server.
     * @param token The new token.
     */
    private boolean sendRegistrationToServer(String token, String salt) {
        Log.d(TAG, "trying to send registration token to server: " + token);
        try {
            String androidId = Settings.Secure.getString(this.getContentResolver(),
                    Settings.Secure.ANDROID_ID);
            // create the json object
            JSONObject json = new JSONObject()
                    .put("user_id", androidId)
                    .put("token", token)
                    .put("salt", salt);
            Log.d(TAG, "json to be sent: " + json.toString());

            RequestSender requestSender = new RequestSender(Config.GCM_REGISTER_URL);
            JSONObject res = requestSender.sendRequest(json);

            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
            sp.edit().putString(Config.SHARED_PREFS_FIREBASE_AUTH, res.getString("token")).apply();

            if(res.has("error")) {
                Log.d(TAG, "an error occurred when trying to send token to the server");
                return false;
            }
            else {
                Log.d(TAG, "successfully sent token to server");
                return true;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Subscribe to any GCM topics of interest, as defined by the TOPICS constant.
     *
     * @param token GCM token
     * @throws IOException if unable to reach the GCM PubSub service
     */
    private void subscribeTopics(String token) throws IOException {
        GcmPubSub pubSub = GcmPubSub.getInstance(this);
        for (String topic : TOPICS) {
            pubSub.subscribe(token, "/topics/" + topic, null);
        }
    }
}
