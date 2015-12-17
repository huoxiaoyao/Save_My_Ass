package ch.ethz.inf.vs.a4.savemyass.P2p;

import org.json.JSONObject;

/**
 * Created by Felix on 17.12.2015.
 */
public final class SendTask {
    public String prefix;
    JSONObject body;

    public SendTask( String prefix, JSONObject body ){
        this.prefix = prefix;
        this.body = body;
    }
}
