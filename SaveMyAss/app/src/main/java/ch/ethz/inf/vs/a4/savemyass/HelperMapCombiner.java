package ch.ethz.inf.vs.a4.savemyass;

import android.location.Location;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import ch.ethz.inf.vs.a4.savemyass.Centralized.Config;
import ch.ethz.inf.vs.a4.savemyass.Structure.HelperInfoBundle;
import ch.ethz.inf.vs.a4.savemyass.Structure.HelperMapUpdateReceiver;

/**
 * Created by jan on 10.12.15.
 *
 * combines the two maps of helper that are on their way.
 */
public class HelperMapCombiner {
    private HashMap<String, HelperInfoBundle> serverMap;
    private HashMap<String, HelperInfoBundle> p2pMap;
    private HashMap<String, UpdateTimeAndSource> lastUpdate;
    private HashMap<String, Location> mergedMap;
    private List<HelperMapUpdateReceiver> mapUpdatReceiver;

    public HelperMapCombiner(){
        serverMap = new HashMap<>();
        p2pMap = new HashMap<>();
        lastUpdate = new HashMap<>();
        mergedMap = new HashMap<>();
        mapUpdatReceiver = new LinkedList<>();
    }

    /**
     * adds the given HelperInfoBundle to the corresponding hashmap and decides whether
     * we need to update the UI.
     * @param centralized true if centralized approach calls this, false if p2p.
     */
    public void add(HelperInfoBundle info, boolean centralized){
        // put the info bundle in the respective map
        if(centralized)
            serverMap.put(info.userID, info);
        else
            p2pMap.put(info.userID, info);

        // logic for updating
        UpdateTimeAndSource update = lastUpdate.get(info.userID);
        if(lastUpdate.containsKey(info.userID) && !update.centralized){
            // if server value is outdated take the p2p value
            if(update.time < info.infoArrivalTime - Config.IGNORE_OTHER_LOCATION_THRESHOLD){
                lastUpdate.put(info.userID, new UpdateTimeAndSource(info.infoArrivalTime, centralized));
                mergedMap.put(info.userID, info.loc);
                notifyReceiversUpdate();
            }
        }
        else{
            mergedMap.put(info.userID, info.loc);
            lastUpdate.put(info.userID, new UpdateTimeAndSource(info.infoArrivalTime, centralized));
            notifyReceiversUpdate();
        }
    }

    /**
     * returns the merged map
     */
    public HashMap<String, Location> getMap(){
        return mergedMap;
    }

    public void register(HelperMapUpdateReceiver newReceiver){
        mapUpdatReceiver.add(newReceiver);
    }

    public void unregsiter(HelperMapUpdateReceiver receiver){
        mapUpdatReceiver.remove(receiver);
    }

    /**
     * notifies the map update receivers
     */
    private void notifyReceiversUpdate(){
        for(HelperMapUpdateReceiver r : mapUpdatReceiver){
            r.onUpdate();
        }
    }

    private class UpdateTimeAndSource{
        public long time;
        public boolean centralized;
        public UpdateTimeAndSource(long time, boolean centralized){
            this.time = time;
            this.centralized = centralized;
        }
    }
}
