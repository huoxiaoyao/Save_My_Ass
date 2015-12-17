package ch.ethz.inf.vs.a4.savemyass.UI;

import android.graphics.Color;
import android.os.Bundle;

import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntroFragment;

import ch.ethz.inf.vs.a4.savemyass.R;

public class Intro extends AppIntro {

    @Override
    public void init(Bundle savedInstanceState) {
        //TODO: set some nice colors
        // Instead of fragments, you can also use our default slide
        // Just set a title, description, background and image. AppIntro will do the rest
        addSlide(AppIntroFragment.newInstance("Welcome to SaveMyAss", "", R.mipmap.ic_launcher, R.color.colorAccent));
        addSlide(AppIntroFragment.newInstance("Geo-Location", "We track your location with GPS. an active internet connection is required.", R.raw.geolocation, R.color.colorAccent));
        addSlide(AppIntroFragment.newInstance("Peer-to-Peer", "In case of an emergency, you can connect to devices near to you.", R.raw.p2p, R.color.colorAccent));
        addSlide(AppIntroFragment.newInstance("Security", "Your location is stored secure and hidden from others.", R.raw.secure, R.color.colorAccent));

        // OPTIONAL METHODS
        // Override bar/separator color
        setBarColor(Color.parseColor("#1A237E"));
        setSeparatorColor(Color.parseColor("#1A237E"));

        // Hide Skip/Done button
        showSkipButton(true);
        showDoneButton(true);

    }

    @Override
    public void onSkipPressed() {
        // Do something when users tap on Skip button.
        finish();
    }

    @Override
    public void onNextPressed() {

    }

    @Override
    public void onDonePressed() {
        // Do something when users tap on Done button.
        finish();
    }

    @Override
    public void onSlideChanged() {

    }
}