package ch.ethz.inf.vs.a4.savemyass.UI;

import android.view.KeyEvent;
import android.view.View;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Created by posedge on 10.12.15.
 * Records and listens for button events to trigger or record an alarm.
 */
public class ButtonCombination implements View.OnKeyListener{

    private LinkedList<ButtonStates> pattern; // pattern of events
    private Iterator<ButtonStates> patternIterator;
    private boolean recording; // recording an new pattern?
    private Runnable trigger; // callback for when the alarm is to be dispatched

    /**
     * Enum to capture states we want to consider for patterns.
     */
    private enum ButtonStates {
        VOLUME_UP("^"),
        VOLUME_DOWN("v");

        private String repr;
        ButtonStates(String repr){
            this.repr = repr;
        }
        public String getRepr(){
            return repr;
        }
    }

    public ButtonCombination(Runnable trigger){
        recording = false;
        pattern = new LinkedList<>();
        this.trigger = trigger;
    }

    public void setTrigger(Runnable trigger){
        this.trigger = trigger;
    }

    /**
     * Two modes:
     *  - recording mode: key patterns are recorded
     *  - alarm mode: key events are matched with the pattern and the alarm is triggered
     * @param recording switch to recording mode if true, switch to alarm mode otherwise
     */
    public void setRecording(boolean recording) {
        if(recording){
            pattern = new LinkedList<>();
        } else {
            patternIterator = pattern.iterator();
        }
        this.recording = recording;
    }

    /**
     * Toggle recording/listening mode
     * @return true if now in recording mode, false if now in listening mode.
     */
    public boolean toggleRecording(){
        setRecording(!recording);
        return recording;
    }

    /**
     * Convert the key code of the last key event to the next state of our pattern.
     * Can depend on the last state and currently pressed buttons, but doesnt have to.
     */
    protected ButtonStates nextState(int keyCode){
        switch(keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                return ButtonStates.VOLUME_UP;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                return ButtonStates.VOLUME_DOWN;
            default:
                return null;
        }
    }

    /*
     * Event handlers.
     */

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if(recording) return recordingModeOnKey(keyCode, event);
        else return alarmModeOnKey(keyCode, event);
    }

    /**
     * Add key state to pattern
     */
    protected boolean recordingModeOnKey(int keyCode, KeyEvent event){
        ButtonStates nextState = nextState(keyCode);
        if(nextState != null){
            pattern.add(nextState);
            return true;
        }
        return false;
    }

    /**
     * Match key state with pattern
     */
    protected boolean alarmModeOnKey(int keyCode, KeyEvent event){
        if(pattern.isEmpty()) return true;

        ButtonStates next = patternIterator.next();
        if(next == nextState(keyCode)){
            // matching the pattern
            if(!patternIterator.hasNext()){
                // whole pattern was matched
                dispatchAlarm();
            }
        } else {
            patternIterator = pattern.iterator();
        }
        return true;
    }

    protected void dispatchAlarm(){
        trigger.run();
    }

    /**
     * @return a String for humans to see the pattern.
     */
    public String visualizePattern(){
        String result = "";
        for (ButtonStates s : pattern) {
            result += s.getRepr();
        }
        return result;
    }
}
