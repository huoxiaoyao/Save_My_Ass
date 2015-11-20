package ch.ethz.inf.vs.a4.savemyass.Structure;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Fabian_admin on 20.11.2015.
 */
public class SimpleAlarmDistributor implements AlarmDistributor{
    //use as delegate to send alarms
    List<AlarmSender> senders;

    public SimpleAlarmDistributor() {
        senders = new LinkedList<AlarmSender>();
    }

    @Override
    public void distributeToSend(InfoBundle info) {
        for(AlarmSender s : senders) {
            s.callForHelp(info);
        }
    }

    @Override
    public void register(AlarmSender client) {
        senders.add(client);
    }

    @Override
    public void deregister(AlarmSender client) {
        senders.remove(client);
    }
}
