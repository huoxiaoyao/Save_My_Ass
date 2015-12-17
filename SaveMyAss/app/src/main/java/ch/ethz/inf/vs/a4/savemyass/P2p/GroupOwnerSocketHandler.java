package ch.ethz.inf.vs.a4.savemyass.P2p;


import android.location.Location;
import android.os.Handler;
import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import ch.ethz.inf.vs.a4.savemyass.Structure.PINInfoBundle;

/**
 * Created by Felix on 13.12.2015.
 */
public class GroupOwnerSocketHandler extends Thread {

    public static final String TAG = "## OwnerHandler ##";

    private ServerSocket socket = null;

    private final P2PMaster master;
    private final SocketListenerBasis socketListenerThread;

    private final List<InetAddress> alarmInvokerList;

    public GroupOwnerSocketHandler( P2PMaster master, int port ){
        this.master = master;

        alarmInvokerList = new ArrayList<>();

        try {
            socket = new ServerSocket( port );

        } catch (IOException e) {
            e.printStackTrace();
        }

        //Todo: add listener for update onto map here
        socketListenerThread = new SocketListenerBasis( master.taskQueue, master.socketList, Arrays.<LocationUpdateListener>asList(  ), master.userID ){

            @Override
            protected void preparation() { }

            @Override
            protected byte[] reactToMessage(String prefix, PINInfoBundle body) {
                //TODO: support other message types for full functionality
                if ( prefix.equals( ConfigP2p.ALARM_INIT ) && !alarmInvokerList.contains( socket.getInetAddress() ) )
                {
                    alarmInvokerList.add(socket.getInetAddress());

                    updateLocation( body );

                    return toBytes( ConfigP2p.ALARM_INIT, body.toJSON() );
                }

                return null;
            }
        };

    }

    /**
     * Assuming that the Group Owner is always the alarm initiator.
     * Sends the position to the alarm receiver.
     * @param connection
     */

    private void initialCommunication( Socket connection ){
        master.addSocket( connection );
    }

    @Override
    public void run() {

        socketListenerThread.start();

        while (true) {
            try
            {
                // TODO: maybe add some timeouts, counter or something similar
                Log.d( TAG, "accepting on server socket" );
                Socket connectionWithClient = socket.accept();
                Log.d( TAG, "sending message" );
                initialCommunication( connectionWithClient );
                Log.d(TAG, "sending successful");
            }
            catch (IOException e)
            {
                e.printStackTrace();
                try
                {
                    if (socket != null && !socket.isClosed())
                        socket.close();
                }
                catch (IOException ioe)
                { }

                break;
            }
        }
    }

}
