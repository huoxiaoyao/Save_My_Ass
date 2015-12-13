package ch.ethz.inf.vs.a4.savemyass.Structure;

import android.location.Location;

/**
 * Created by jan on 10.12.15.
 *
 * stores info about the helper on their way including the time we got this information.
 */
public class HelperInfoBundle {
    public final String userID;
    public final Location loc;
    public final long infoArrivalTime; // miliseconds system time

    public HelperInfoBundle(String userID, Location loc, long infoArrivalTime){
        this.userID = userID;
        this.loc = loc;
        this.infoArrivalTime = infoArrivalTime;
    }
}
