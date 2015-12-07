package ch.ethz.inf.vs.a4.savemyass.Centralized;

import org.json.JSONObject;

/**
 * Created by jan on 07.12.15.
 *
 * interface for receiving of a http response
 */
public interface ResponseListener {
    void onResponseReceive(JSONObject json);
}
