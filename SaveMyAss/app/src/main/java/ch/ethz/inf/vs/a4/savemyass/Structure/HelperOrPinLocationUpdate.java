package ch.ethz.inf.vs.a4.savemyass.Structure;

import android.location.Location;

/**
 * Created by jan on 12.12.15.
 *
 * Interface for handling location updates from a client from a client
 */
public interface HelperOrPinLocationUpdate {
    void onLocationUpdate(Location loc);
}
