package ch.ethz.inf.vs.a4.savemyass.Structure;

/**
 * Created by Fabian_admin on 20.11.2015.
 */
public interface AlarmDistributor {
    //this class should be implemented by the class which recognizes that an alarm was triggered
    //use SimpleAlarmDistributor as a delegate
    void distributeToSend(PINInfoBundle info);
    void register(AlarmSender client);
    void deregister(AlarmSender client);
}
