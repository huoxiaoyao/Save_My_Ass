package ch.ethz.inf.vs.a4.savemyass;


import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import ch.ethz.inf.vs.a4.savemyass.Centralized.Config;
import ch.ethz.inf.vs.a4.savemyass.Centralized.GCMRegistrationIntentService;
import ch.ethz.inf.vs.a4.savemyass.UI.Intro;

public class MainActivity extends AppCompatActivity
    implements NavigationView.OnNavigationItemSelectedListener {

    protected final String TAG = "###MainActivity";

    private static final int ACCOUNTS_PERMISSION = 1;
    private static final int LOCATION_PERMISSION = 2;

    private BroadcastReceiver RegistrationBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Android 6+ permission checks...
        boolean location, contacts;
        location = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        contacts = ContextCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS)
                == PackageManager.PERMISSION_GRANTED;
        if (!(location && contacts)){
            if(!location)
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION);
            else
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.GET_ACCOUNTS}, ACCOUNTS_PERMISSION);
        }
        else {
            // only gets executed if all permissions are set
            initializeCentralized();
            Intent i = new Intent(getApplicationContext(), BackgroundService.class);
            startService(i);

            // alarm trigger button
            Button alarm = (Button) findViewById(R.id.triggerAlarm);
            alarm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showSimplePopUp();
                }
            });
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        SharedPreferences sp =  PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        sp.edit().putBoolean(Config.SHARED_PREFS_ALARM_ACTIVE, false).apply();
        boolean info = sp.getBoolean(Config.SHARED_PREFS_FIRSTOPEN, true);
        if(info){
            sp.edit().putBoolean(Config.SHARED_PREFS_FIRSTOPEN, false).apply();
            Intent i = new Intent(getApplicationContext(), Intro.class);
            startActivity(i);
        }
    }

    @Override
    protected void onStart() {
        // check if location service is enabled, otherwise guide the user to the respective settings
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {
            ex.printStackTrace();
        }

        if(!gps_enabled) {
            // notify user
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setMessage(getString(R.string.gps_network_not_enabled));
            dialog.setPositiveButton(getString(R.string.open_location_settings), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    Intent i = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(i);
                }
            });
            dialog.setNegativeButton(getString(R.string.dont_open_location_settings), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    finish();
                }
            });
            dialog.show();
        }
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(RegistrationBroadcastReceiver,
                new IntentFilter(Config.REGISTRATION_COMPLETE_BROADCAST));
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(RegistrationBroadcastReceiver);
        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        this.recreate();
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * does the initialization that is necessary for the centralized approach
     */
    private void initializeCentralized() {
        // if we didn't already sent the user-id to the server, we do it now!
        // note SENT_TOKEN_TO_SERVER set to true also guarantees that there is a user id in the
        // shared prefs
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        //sharedPreferences.edit().putBoolean(Config.SENT_TOKEN_TO_SERVER, false).apply();

        // progress bar that is used for the registration for the centralized approach
        // todo add the progress bar to the layout
        //RegistrationProgressBar = (ProgressBar) findViewById(R.id.registrationProgressBar);
        if(!sharedPreferences.getBoolean(Config.SENT_TOKEN_TO_SERVER, false)){
            // broadcast receiver that gets event as soon as server responded...
            RegistrationBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    //RegistrationProgressBar.setVisibility(ProgressBar.GONE);
                    SharedPreferences sp =  PreferenceManager.getDefaultSharedPreferences(context);
                    boolean sentToken = sp.getBoolean(Config.SENT_TOKEN_TO_SERVER, false);
                    boolean centralized = sp.getBoolean(Config.SHARED_PREFS_CENTRALIZED_ACTIVE, false);
                    if(centralized && !sentToken){
                        String text = getString(R.string.restart_with_internet);
                        Toast t = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG);
                        t.show();
                    }
                }
            };
            Intent i = new Intent(this, GCMRegistrationIntentService.class);
            startService(i);
            Log.d(TAG, "GCMRegistrationIntentService has been started");
        }
        else{
            //RegistrationProgressBar.setVisibility(ProgressBar.GONE);
        }
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.logo) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.on_off_mode) {
            Intent intent = new Intent(this, On_OffMode.class);
            startActivity(intent);

        } else if (id == R.id.personal_message) {
            Intent intent = new Intent(this, CustomMessage.class);
            startActivity(intent);

        } else if(id == R.id.info){
            Intent i = new Intent(getApplicationContext(), Intro.class);
            startActivity(i);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showSimplePopUp() {

        AlertDialog.Builder helpBuilder = new AlertDialog.Builder(this);
        helpBuilder.setTitle("Set alarm?");

        helpBuilder.setNeutralButton("Cancel",
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        // nothing here
                    }
                }
        );

        helpBuilder.setPositiveButton("Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        openMap();
                    }

                });

        // Remember, create doesn't show the dialog
        AlertDialog helpDialog = helpBuilder.create();
        helpDialog.show();
    }

    public void openMap(){
        Intent intent = new Intent(this, HelpRequest.class);
        startActivity(intent);
    }

    public void customMessageClick(View v){

    }
}
