package ch.ethz.inf.vs.a4.savemyass.P2p;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

public class MainActivity extends AppCompatActivity {

    private ToggleButton alarmButton;
    private P2PMaster master;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        alarmButton = (ToggleButton) findViewById(R.id.alarmButton);

        master = P2PMaster.createP2PMaster( this );
        master.acceptAlarms();

        alarmButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    master.invokeAlarm();
                } else {
                    master.revokeAlarm();
                }
            }
        });
    }

    public void test(View view) {
        Intent i = new Intent(this, ArrowActivity.class);
        startActivity(i);
    }
}
