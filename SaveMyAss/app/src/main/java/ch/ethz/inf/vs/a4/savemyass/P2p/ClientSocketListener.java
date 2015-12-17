package ch.ethz.inf.vs.a4.savemyass.P2p;

import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ch.ethz.inf.vs.a4.savemyass.Structure.PINInfoBundle;

/**
 * Created by Felix on 17.12.2015.
 */
public class ClientSocketListener extends SocketListenerBasis {

    public static final String TAG = "## ClientSocket ##";

    private final InetAddress groupOwnerAddress;
    private final int serverPort;
    private final int port;

    private final P2PMaster master;

    public ClientSocketListener( P2PMaster master, InetAddress groupOwnerAddress, int serverPort,int  port ) {
        //Todo: add listener for update onto map here

        super( master.taskQueue, master.socketList, Arrays.<LocationUpdateListener>asList(), master.userID );

        this.master = master;
        this.groupOwnerAddress = groupOwnerAddress;
        this.serverPort = serverPort;
        this.port = port;
    }

        @Override
    protected void preparation() {
            Socket socket = new Socket();
            try
            {
                Log.d(TAG, "connecting on socket");
                socket.bind(null);
                socket.connect(new InetSocketAddress(groupOwnerAddress.getHostAddress(), serverPort), port);

                master.addSocket(socket);
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

    @Override
    protected byte[] reactToMessage( String prefix, PINInfoBundle body ) {
        //TODO: support other message types for full functionality
        if ( prefix.equals( ConfigP2p.ALARM_INIT ) )
        {
            Log.d(TAG, "received new alarm");
                updateLocation( body );
        }

        return null;
    }
}
