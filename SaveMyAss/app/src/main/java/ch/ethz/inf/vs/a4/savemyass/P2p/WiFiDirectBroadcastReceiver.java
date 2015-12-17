package ch.ethz.inf.vs.a4.savemyass.P2p;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

/**
 * Created by Felix on 13.12.2015.
 */
class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    public final static String TAG = "## BroadcastReceiver ##";

    private final WifiP2pManager manager;
    private final WifiP2pManager.Channel channel;

    private final WifiP2pManager.ConnectionInfoListener connectionInfoListener;

    public WiFiDirectBroadcastReceiver( WifiP2pManager manager, WifiP2pManager.Channel channel, WifiP2pManager.ConnectionInfoListener connectionInfoListener ){
        this.manager = manager;
        this.channel = channel;
        this.connectionInfoListener = connectionInfoListener;
    }

    /**
     * Receiver handles the following actions:
     *
     * - WIFI_P2P_CONNECTION_CHANGED_ACTION:
     *  if connection action then request connection info else do nothing
     *
     */

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();

        switch( action ){

            case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION:
                // TODO:  specify if there is something here to do
                break;

            case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:

                if ( manager != null )
                {
                    NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                    if ( networkInfo.isConnected() )
                    {
                        Log.d(TAG, "Connected to some device");
                        manager.requestConnectionInfo( channel, connectionInfoListener );
                    }
                    else
                    {
                        // TODO:  specify if there is something here to do, maybe notify P2PMaster
                        Log.d( TAG, "Disconnected from some device" );
                    }
                }

                break;

            case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION:
                // TODO:  specify if there is something here to do
                break;

            case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION:
                // TODO:  specify if there is something here to do
                break;
        }
    }
}
