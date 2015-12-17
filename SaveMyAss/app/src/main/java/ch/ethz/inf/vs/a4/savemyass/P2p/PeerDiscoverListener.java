package ch.ethz.inf.vs.a4.savemyass.P2p;

import android.net.wifi.p2p.WifiP2pDevice;

/**
 * Created by Fabian_admin on 16.12.2015.
 */
interface PeerDiscoverListener {
    public void foundPeerDevice(WifiP2pDevice device);
}
