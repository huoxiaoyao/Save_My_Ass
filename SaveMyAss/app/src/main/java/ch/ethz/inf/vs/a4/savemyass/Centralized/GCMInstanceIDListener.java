package ch.ethz.inf.vs.a4.savemyass.Centralized;

import android.content.Intent;

import com.google.android.gms.iid.InstanceIDListenerService;

/**
 * Created by jan on 03.12.15.
 *
 * what the heck is this used for?
 *
 */
public class GCMInstanceIDListener extends InstanceIDListenerService {
    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. This call is initiated by the
     * InstanceID provider.
     */
    @Override
    public void onTokenRefresh() {
        // Fetch updated Instance ID token and notify our app's server of any changes (if applicable).
        Intent intent = new Intent(this, GCMRegistrationIntentService.class);
        intent.putExtra(Config.INTENT_NEW_TOKEN, true);
        startService(intent);
    }
}
