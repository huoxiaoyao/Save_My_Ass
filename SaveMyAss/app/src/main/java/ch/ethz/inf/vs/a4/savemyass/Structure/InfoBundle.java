package ch.ethz.inf.vs.a4.savemyass.Structure;

import android.location.Location;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Fabian_admin on 20.11.2015.
 */
public class InfoBundle {

    //TODO: insert all the attributes we need: position etc
    private final String userID;
    private final Location loc;

    public InfoBundle(String userID, Location loc){
        this.userID = userID;
        this.loc = loc;
    }

    public JSONObject toJSON() {
        JSONObject json = null;
        try {
            if(loc == null)
                return null;
            json = new JSONObject()
                    .put("location", loc.toString())
                    .put("user_id", userID);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public String fromJSON() {
        return null;
    }
}

