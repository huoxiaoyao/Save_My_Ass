package ch.ethz.inf.vs.a4.savemyass;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

public class HelpRequest extends AppCompatActivity {

    private TextView log;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help_request);
        log = (TextView) findViewById(R.id.help_request_log);
        log.setText(log.getText()+"\n- alarm!");
    }
}
