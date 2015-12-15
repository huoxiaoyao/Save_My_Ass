package ch.ethz.inf.vs.a4.savemyass;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import ch.ethz.inf.vs.a4.savemyass.Centralized.Config;

public class CustomMessage extends AppCompatActivity {

    protected SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_message);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        sp = PreferenceManager.getDefaultSharedPreferences(this);
        final EditText customMessage = (EditText) findViewById(R.id.customeMessage);
        customMessage.setText(sp.getString(Config.SHARED_PREFS_USER_MESSAGE, ""));
        Button save = (Button) findViewById(R.id.save);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                sp.edit().putString(Config.SHARED_PREFS_USER_MESSAGE, customMessage.getText().toString()).apply();
                finish();
            }
        });
    }

}
