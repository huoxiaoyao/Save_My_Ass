package ch.ethz.inf.vs.a4.savemyass.P2p;

import android.app.Activity;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.provider.Settings;
import android.util.Log;

import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import ch.ethz.inf.vs.a4.savemyass.Structure.AlarmDistributor;
import ch.ethz.inf.vs.a4.savemyass.Structure.AlarmSender;
import ch.ethz.inf.vs.a4.savemyass.Structure.PINInfoBundle;
import ch.ethz.inf.vs.a4.savemyass.Structure.ServiceDestroyReceiver;
import ch.ethz.inf.vs.a4.savemyass.Structure.SimpleAlarmDistributor;

/**
 * Created by Fabian_admin on 16.12.2015.
 *
 * onesided peer discovery did not work reliable, hence testing was horrible.
 * The main structure does exist and work now,
 * but we did not integrate this completely into the GUI/Application
 *
 */
public class P2PMaster implements WifiP2pManager.ConnectionInfoListener, PeerDiscoverListener,
        ServiceDestroyReceiver, AlarmDistributor, AlarmSender {
    public static final String TAG = "## P2PMaster ##";

    public static String userID;
    private String userMessage;

    //delegate
    private AlarmDistributor myAlarmDistributor;

    protected final WifiP2pManager manager;
    protected final WifiP2pManager.Channel channel;

    private WiFiDirectBroadcastReceiver broadcastReceiver;

    public static final String TXTRECORD_PROP_AVAILABLE = "available";
    public static final String SERVICE_INSTANCE = "_safeMyAss";
    public static final String SERVICE_REG_TYPE = "_presence._tcp";

    private static final int SERVER_PORT = 4545;
    private static final int PORT = 5000;

    protected final PeerDiscovery peerDiscovery;

    private final WifiP2pDnsSdServiceInfo service;

    public final LinkedBlockingQueue<Socket> socketList;
    public final ArrayBlockingQueue<SendTask> taskQueue;

    private boolean alarmActive = false;

    public static P2PMaster lastInstance = null;

    public static P2PMaster createP2PMaster( final Context context ){

        P2PMaster master = new P2PMaster( context );

        WiFiDirectBroadcastReceiver broadcastReceiver = new WiFiDirectBroadcastReceiver( master.manager, master.channel, master );

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        master.setBroadcastReceiver(broadcastReceiver);
        context.registerReceiver(broadcastReceiver, intentFilter);

        lastInstance = master;
        return master;
    }

    private P2PMaster( final Context context ){

        userID = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

        //as a delegate
        myAlarmDistributor = new SimpleAlarmDistributor();

        manager = (WifiP2pManager) context.getSystemService( context.WIFI_P2P_SERVICE );
        channel = manager.initialize( context, context.getMainLooper(), null );

        peerDiscovery = new PeerDiscovery( manager, channel, SERVICE_INSTANCE );

        Map<String, String> record = new HashMap<String, String>();
        record.put(TXTRECORD_PROP_AVAILABLE, "visible");
        // TODO: maybe add more record entries

        service = WifiP2pDnsSdServiceInfo.newInstance( SERVICE_INSTANCE, SERVICE_REG_TYPE, record );

        socketList = new LinkedBlockingQueue<Socket>( ConfigP2p.SOCKET_QUEUE_SIZE );
        taskQueue = new ArrayBlockingQueue<SendTask>( ConfigP2p.SEND_TASK_QUEUE_SIZE );

    }

    private void setBroadcastReceiver( WiFiDirectBroadcastReceiver broadcastReceiver ){
        this.broadcastReceiver = broadcastReceiver;
    }

    public void invokeAlarm(){
        alarmActive = true;
        peerDiscovery.registerListener( this );
        peerDiscovery.startDiscovery();
    }

    public void revokeAlarm(){
        //TODO
        alarmActive = false;
    }

    public void acceptAlarms(){
        manager.clearLocalServices(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                manager.addLocalService(channel, service, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Log.d( TAG, "add service success");
                    }

                    @Override
                    public void onFailure(int reason) {
                        // TODO: add some failure mechanism
                        Log.d(TAG, "add service failure");
                    }
                });
            }

            @Override
            public void onFailure(int reason) {
                // TODO: maybe add some failure mechanism
                Log.d( TAG, "add service failure");
            }
        });
    }

    public void setUserMessage(String message) {
        userMessage = message;
    }

    public void cleanup() {
        if(manager != null) {
            manager.clearLocalServices(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "remove service success");
                }

                @Override
                public void onFailure(int reason) {
                    Log.d(TAG, "remove service failure");
                }
            });

            manager.clearServiceRequests(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "remove service request success");
                }

                @Override
                public void onFailure(int reason) {
                    Log.d(TAG, "remove service request failure");
                }
            });

        }
    }

    public void rejectAlarms(){
        manager.clearLocalServices(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "remove service success");
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "remove service failure");
            }
        });
    }

    public void addSocket( Socket socket ){
        socketList.offer(socket);
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        Log.d(TAG, "onConnectionInfoAvailable");

        // TODO: sometimes this method is called even though no connection exists, which leads to errors in the current version

        if(info.groupFormed) {
            if (info.isGroupOwner) {
                new GroupOwnerSocketHandler(this, SERVER_PORT).start();
            } else {
                new ClientSocketListener(this, info.groupOwnerAddress, SERVER_PORT, PORT).start();
            }
        }
    }

    @Override
    public void foundPeerDevice(WifiP2pDevice device) {
        /*
        manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                manager.createGroup(channel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {

                        Log.d(TAG, "create Group success");

                        */

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        config.groupOwnerIntent = 15;

        manager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // TODO: specify if there is something here to do
                Log.d(TAG, "try to connect success");
            }

            @Override
            public void onFailure(int reason) {
                // TODO: add some failure mechanism
                Log.d(TAG, "try to connect failure");
            }
        });
                        /*
                    }

                    @Override
                    public void onFailure(int reason) {
                        Log.d(TAG, "create Group failure");
                    }
                });
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "remove Group failure");


            }
        });
        */
    }

    @Override
    public void onServiceDestroy() {
        lastInstance = null;
        cleanup();
    }

    @Override
    public void distributeToSend(PINInfoBundle info) {
        myAlarmDistributor.distributeToSend(info);
    }

    @Override
    public void register(AlarmSender client) {
        myAlarmDistributor.register(client);
    }

    @Override
    public void deregister(AlarmSender client) {
        myAlarmDistributor.deregister(client);
    }

    @Override
    public void callForHelp(PINInfoBundle bundle) {
        //TODO: comment in when continue working
        // taskQueue.offer(new SendTask(ConfigP2p.ALARM_INIT, bundle.toJSON()));
    }
}
