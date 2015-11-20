package ch.ethz.inf.vs.a4.savemyass.Structure;

/**
 * Created by Fabian_admin on 20.11.2015.
 */
public interface AlarmSender {
    //should be implemented by P2P, centralised, or appropriate classes
    void callForHelp(InfoBundle bundle);
}
