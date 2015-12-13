package ch.ethz.inf.vs.a4.savemyass.UI;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;

/**
 * Created by posedge on 11.12.15.
 */
public class MediaButtonObserver extends ContentObserver {
    private final static String TAG = "###MediaButtonReceiver";
    private AudioManager am;
    private int lastLevel;
    private int maxLevel;
    private boolean ignoreNext = false;
    private OnVolumeChangeListener listener;

    public MediaButtonObserver(Handler handler, Context context, OnVolumeChangeListener listener) {
        super(handler);
        this.am = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        lastLevel = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        maxLevel = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        this.listener  = listener;
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        Log.d("###MediaButtonObserver", "onChange called");
        if(!uri.toString().equals("content://settings/system/volume_music_speaker")){
            super.onChange(selfChange, uri);
            return;
        }
        if(ignoreNext){
            ignoreNext = false;
            super.onChange(selfChange, uri);
            return;
        }
        int level = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        if(level < lastLevel){
            onVolumeChange(false);
        }else{
            onVolumeChange(true);
        }
        if(level == maxLevel){
            level -= 1;
            am.setStreamVolume(AudioManager.STREAM_MUSIC, level, 0);
            ignoreNext = true;
        }else if(level == 0) {
            level += 1;
            am.setStreamVolume(AudioManager.STREAM_MUSIC, level, 0);
            ignoreNext = true;
        }
        lastLevel = level;
        super.onChange(selfChange, uri);
    }

    private void onVolumeChange(boolean up){
        listener.onVolumeChange(up);
    }

    public interface OnVolumeChangeListener{
        void onVolumeChange(boolean up);
    }
}
