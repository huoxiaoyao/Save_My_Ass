package ch.ethz.inf.vs.a4.savemyass.P2p;

import android.location.Location;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import ch.ethz.inf.vs.a4.savemyass.Structure.PINInfoBundle;

/**
 * Created by Felix on 17.12.2015.
 */
public abstract class SocketListenerBasis extends Thread {

    private final ArrayBlockingQueue<SendTask> taskQueue;
    private final LinkedBlockingQueue<Socket> socketList;
    private final List<LocationUpdateListener> locationUpdateListenerList;

    private final List<InetAddress> alarmInvoker;

    private volatile boolean stopVar = false;

    private byte[] buffer;

    private final String ownUserID;

    public SocketListenerBasis( ArrayBlockingQueue<SendTask> taskQueue, LinkedBlockingQueue<Socket> socketList, List<LocationUpdateListener> locationUpdateListenerList, String userID ){

        this.taskQueue = taskQueue;
        this.socketList = socketList;
        this.locationUpdateListenerList = locationUpdateListenerList;
        this.ownUserID = userID;

        buffer = new byte[ ConfigP2p.BUFFER_SIZE ];
        alarmInvoker = new java.util.ArrayList<>( );
    }

    /**
     * Method is called before while(true) loop.
     * Should contain all the necessary preparation
     */

    protected abstract void preparation();

    /**
     * Specifies action on a received message = ( prefix, body )
     * return message that should be send on all available sockets.
     * return null if no message should be send
     * @param prefix
     * @param body
     * @return
     */

    protected abstract byte[] reactToMessage( String prefix, PINInfoBundle body );

    public void stopLooping(){
        stopVar = false;
    }

    public static byte[] toBytes( String prefix, JSONObject body ){
        return ( prefix + body.toString().length() + ConfigP2p.JSON_LENGTH_DELIMITER + body.toString() ).getBytes();
    }

    protected void sendAll( byte[] message ){

        byte[] sendMessage = Arrays.copyOf( message, ConfigP2p.MESSAGE_SIZE_LONG );

        for( Socket socket : socketList ){
            try
            {
                socket.getOutputStream().write( message );
                socket.getOutputStream().flush();
            }
            catch (IOException e)
            {
                try
                {
                    socket.close();
                }
                catch (IOException e2)
                {
                    e.printStackTrace();
                }
                finally
                {
                    socketList.remove( socket );
                }
            }
        }
    }

    protected void updateLocation( PINInfoBundle bundle ){
        for ( LocationUpdateListener listener : locationUpdateListenerList ){
            listener.onLocationUpdate( bundle );
        }
    }



    public final void run() {

        preparation();

        while ( !this.stopVar ) {
            long start_time = System.currentTimeMillis();

            if ( !taskQueue.isEmpty() )
            {
                try
                {
                    SendTask sendTask = taskQueue.take();
                    sendAll( toBytes( sendTask.prefix, sendTask.body ) );
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }

            for ( Socket socket : socketList ){
                try
                {
                    if ( socket.getInputStream().available() != 0 )
                    {
                        socket.getInputStream().read( buffer );
                        String message = new String( buffer, 0, ConfigP2p.MESSAGE_SIZE_SHORT );
                        String prefix = message.substring( 0, ConfigP2p.PREFIX_LENGTH );
                        String rest = message.substring(ConfigP2p.PREFIX_LENGTH, message.length());
                        Log.d( "## SocketBasis ##", "received whole: " + rest );
                        String[] args = rest.split( ConfigP2p.JSON_LENGTH_DELIMITER );
                        int length = Integer.parseInt(args[0]);
                        String json = args[1].substring(0, length);
                        Log.d( "## SocketBasis ##", "received " + json );
                        JSONObject body = new JSONObject( json );

                        String userID = body.getString("user_id");

                        if ( !ownUserID.equals( userID ) ){

                            JSONObject location = body.getJSONObject("location");
                            double longitude = location.getDouble("long");
                            double latitude = location.getDouble("lat");
                            String msg = body.getString( "msg" );

                            Location alarmLocation = new Location("");
                            alarmLocation.setLongitude(longitude);
                            alarmLocation.setLatitude(latitude);

                            byte[] reaction = reactToMessage( prefix, new PINInfoBundle( userID, alarmLocation, msg ) );
                            if ( reaction != null )
                            {
                                sendAll( reaction );
                            }
                        }
                    }
                }
                catch ( IOException e )
                {
                    e.printStackTrace();
                }
                catch (JSONException e)
                {
                    e.printStackTrace();
                }
            }

            long end_time = System.currentTimeMillis();
            long elapsed_ns = end_time - start_time;
            if ( elapsed_ns < ConfigP2p.MIN_TIME_THRESHOLD) {
                try {
                    Thread.sleep(ConfigP2p.MIN_TIME_THRESHOLD - elapsed_ns);
                } catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

}
