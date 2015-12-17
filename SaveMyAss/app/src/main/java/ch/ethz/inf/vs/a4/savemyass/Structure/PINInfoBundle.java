package ch.ethz.inf.vs.a4.savemyass.Structure;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Fabian_admin on 20.11.2015.
 */
public class PINInfoBundle implements Parcelable {

    public String userID;
    public Location loc;
    public String message;

    public PINInfoBundle(String userID, Location loc, String message){
        this.userID = userID;
        this.loc = loc;
        this.message = message;
    }

    private PINInfoBundle(Parcel in) {
        this.userID = in.readString();
        this.loc = in.readParcelable(Location.class.getClassLoader());
        this.message = in.readString();
    }

    public JSONObject toJSON() {
        JSONObject json = null;
        try {
            if(loc == null)
                return null;
            json = new JSONObject()
                    .put("user_id", userID)
                    .put("location", new JSONObject()
                            .put("long", loc.getLongitude())
                            .put("lat", loc.getLatitude()))
                    .put("msg", message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public String fromJSON() {
        return null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(userID);
        dest.writeParcelable(loc, flags);
        dest.writeString(message);
    }

    public static final Parcelable.Creator<PINInfoBundle> CREATOR
            = new Parcelable.Creator<PINInfoBundle>() {
        public PINInfoBundle createFromParcel(Parcel in) {
            return new PINInfoBundle(in);
        }

        public PINInfoBundle[] newArray(int size) {
            return new PINInfoBundle[size];
        }
    };
}

