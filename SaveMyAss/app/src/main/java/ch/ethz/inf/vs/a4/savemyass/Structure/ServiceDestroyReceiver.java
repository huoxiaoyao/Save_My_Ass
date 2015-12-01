package ch.ethz.inf.vs.a4.savemyass.Structure;

/**
 * Created by jan on 01.12.15.
 *
 * simple interface that allows the background service to clean stuff up when it gets destroyed.
 */
public interface ServiceDestroyReceiver {
    void onServiceDestroy();
}
