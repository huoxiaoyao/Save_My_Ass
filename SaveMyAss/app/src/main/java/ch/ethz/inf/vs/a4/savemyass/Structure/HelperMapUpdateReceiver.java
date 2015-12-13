package ch.ethz.inf.vs.a4.savemyass.Structure;

/**
 * Created by jan on 10.12.15.
 *
 * this receiver should be implemented by the UI to get the updates of the helper list
 */
public interface HelperMapUpdateReceiver {

    // gets called when the helper map got updated...
    void onUpdate();
}
