package ch.ethz.inf.vs.a4.savemyass;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.CompoundButton;
import android.widget.Switch;

import ch.ethz.inf.vs.a4.savemyass.Centralized.Config;

public class On_OffMode extends AppCompatActivity {

    protected SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_on__off_mode);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Switch p2pConnection = (Switch) findViewById(R.id.p2pConnection);
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        //just until it works
        sp.edit().putBoolean(Config.SHARED_PREFS_P2P_ACTIVE, false).apply();
        p2pConnection.setChecked(sp.getBoolean(Config.SHARED_PREFS_P2P_ACTIVE, false));
        p2pConnection.setEnabled(false);
        p2pConnection.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sp.edit().putBoolean(Config.SHARED_PREFS_P2P_ACTIVE, isChecked).apply();
            }
        });

        Switch serverConnection = (Switch) findViewById(R.id.serverConnection);
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        serverConnection.setChecked(sp.getBoolean(Config.SHARED_PREFS_CENTRALIZED_ACTIVE, true));
        serverConnection.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sp.edit().putBoolean(Config.SHARED_PREFS_CENTRALIZED_ACTIVE, isChecked).apply();
            }
        });

    }

}
