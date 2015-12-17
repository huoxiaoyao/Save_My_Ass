package ch.ethz.inf.vs.a4.savemyass.P2p;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.util.Log;

import java.util.List;
import java.util.Map;

/**
 * Created by Fabian_admin on 16.12.2015.
 */
public class PeerDiscovery {
    public final static String TAG = "## PeerDiscovery ##";

    private final  WifiP2pManager manager;
    private final WifiP2pManager.Channel channel;

    private final String serviceInstance;

    private final List<PeerDiscoverListener> registeredListener;

    public PeerDiscovery( WifiP2pManager manager, WifiP2pManager.Channel channel, String serviceInstance ){
        this.manager = manager;
        this.channel = channel;
        this.serviceInstance = serviceInstance;

        registeredListener = new java.util.ArrayList<>();
    }

    public void registerListener( PeerDiscoverListener listener ){
        registeredListener.add( listener );
    }

    public void unregisterListener( PeerDiscoverListener listener ){
        registeredListener.remove(listener);
    }

    private synchronized void notifyListener( WifiP2pDevice foundDevice ){
        for ( PeerDiscoverListener listener : registeredListener ){
            listener.foundPeerDevice( foundDevice );
        }
    }


    public void startDiscovery(){
        preDiscovery();
    }

    public void stopDiscovery(){
        manager.clearServiceRequests(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(int reason) {

            }
        });
    }

    /**
     * preDiscovery makes preparations for the peerDiscovery and if successful calls discoverPeers()
     */

    private void preDiscovery(){
        manager.clearServiceRequests( channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                discoverPeers();
            }

            @Override
            public void onFailure(int reason) {
                // TODO: add some failure mechanism
            }
        });
    }

    private void discoverPeers(){

        WifiP2pManager.DnsSdServiceResponseListener dnsSdServiceResponseListener = new WifiP2pManager.DnsSdServiceResponseListener() {

            @Override
            public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice srcDevice) {

                if (instanceName.equalsIgnoreCase( serviceInstance ))
                {
                    Log.d(TAG, "found Device " + instanceName);
                    notifyListener( srcDevice );
                }
            }
        };

        WifiP2pManager.DnsSdTxtRecordListener dnsSdTxtRecordListener = new WifiP2pManager.DnsSdTxtRecordListener() {

            /**
             * A new TXT record is available. Pick up the advertised
             * buddy name.
             */
            @Override
            public void onDnsSdTxtRecordAvailable(String fullDomainName, Map<String, String> record, WifiP2pDevice device) {
                // TODO: specify if there is something here to do, maybe check some record entries...
            }
        };

        manager.setDnsSdResponseListeners( channel, dnsSdServiceResponseListener, dnsSdTxtRecordListener );
        WifiP2pDnsSdServiceRequest serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        manager.addServiceRequest(channel, serviceRequest, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Request add success");
                manager.discoverServices(channel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        // TODO: specify if there is something here to do
                        Log.d(TAG, "Service discovery success");
                    }

                    @Override
                    public void onFailure(int reason) {
                        // TODO: add some failure mechanism
                        Log.d(TAG, "Service discovery failure");
                    }
                });
            }

            @Override
            public void onFailure(int reason) {
                // TODO: add some failure mechanism
                Log.d(TAG, "Request add failure");
            }
        });
        // TODO: maybe add some handler.postDelayed to stop discovery

    }
}
