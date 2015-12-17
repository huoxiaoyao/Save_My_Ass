package ch.ethz.inf.vs.a4.savemyass.P2p;


import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by Felix on 13.12.2015.
 */
public class ClientSocketHandler extends Thread {

    public static final String TAG = "## ClientSocket ##";

    private final InetAddress groupOwnerAddress;
    private final int serverPort;
    private final int port;

    private final P2PMaster master;

    public ClientSocketHandler( P2PMaster master, InetAddress groupOwnerAddress, int serverPort,int  port ) {
        this.master = master;
        this.groupOwnerAddress = groupOwnerAddress;
        this.serverPort = serverPort;
        this.port = port;
    }

    @Override
    public void run() {
        Socket socket = new Socket();
        try
        {
            Log.d( TAG, "connecting on socket" );
            socket.bind(null);
            socket.connect( new InetSocketAddress(groupOwnerAddress.getHostAddress(), serverPort), port );

            InputStream iStream = socket.getInputStream();
            byte[] buffer = new byte[1024];

            Log.d( TAG, "waiting for message" );
            int bytes = iStream.read( buffer );

            String receivedMessage = new String( buffer, 0, bytes );
            Log.d( TAG, "received: " + receivedMessage );

            // TODO: specify what has to be done next
        }
        catch (IOException e)
        {
            e.printStackTrace();
            try
            {
                socket.close();
            } catch (IOException e1)
            {
                e1.printStackTrace();
            }
            return;
        }
    }

}
