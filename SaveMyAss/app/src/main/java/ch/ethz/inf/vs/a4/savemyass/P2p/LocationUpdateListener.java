package ch.ethz.inf.vs.a4.savemyass.P2p;

import android.location.Location;

import ch.ethz.inf.vs.a4.savemyass.Structure.PINInfoBundle;

/**
 * Created by Felix on 17.12.2015.
 */
public interface LocationUpdateListener {

    public void onLocationUpdate( PINInfoBundle bundle);

}
