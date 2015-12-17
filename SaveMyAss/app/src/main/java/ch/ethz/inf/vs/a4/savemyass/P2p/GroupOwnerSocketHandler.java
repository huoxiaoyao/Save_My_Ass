package ch.ethz.inf.vs.a4.savemyass.P2p;


import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Felix on 13.12.2015.
 */
public class GroupOwnerSocketHandler extends Thread {

    public static final String TAG = "## OwnerHandler ##";

    private ServerSocket socket = null;

    private final P2PMaster master;

    public GroupOwnerSocketHandler( P2PMaster master, int port ){
        this.master = master;

        try {
            socket = new ServerSocket( port );

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Assuming that the Group Owner is always the alarm initiator.
     * Sends the position to the alarm receiver.
     * @param connection
     */

    private void initialCommunication( Socket connection ){
        try
        {
            // TODO: read out position and save in message
            byte[] message = "magical coordinates".getBytes();
            connection.getOutputStream().write( message );
            master.addSocket( connection );
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                socket.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        master.addSocket( connection );
    }

    @Override
    public void run() {
        while (true) {
            try
            {
                // TODO: maybe add some timeouts, counter or something similar
                Log.d( TAG, "accepting on server socket" );
                Socket connectionWithClient = socket.accept();
                Log.d( TAG, "sending message" );
                initialCommunication(connectionWithClient);
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
