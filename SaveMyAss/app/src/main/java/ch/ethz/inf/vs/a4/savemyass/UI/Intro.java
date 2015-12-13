package ch.ethz.inf.vs.a4.savemyass.UI;

import android.graphics.Color;
import android.os.Bundle;

import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntroFragment;

import ch.ethz.inf.vs.a4.savemyass.R;

public class Intro extends AppIntro {

    @Override
    public void init(Bundle savedInstanceState) {

        // Instead of fragments, you can also use our default slide
        // Just set a title, description, background and image. AppIntro will do the rest
        addSlide(AppIntroFragment.newInstance("Geo-Location", "DESC", R.raw.geolocation, R.color.colorAccent));
        addSlide(AppIntroFragment.newInstance("Peer to Peer", "DESC", R.raw.p2p, R.color.colorAccent));
        addSlide(AppIntroFragment.newInstance("Security", "DESC", R.raw.secure, R.color.colorAccent));

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
    }

    @Override
    public void onNextPressed() {

    }

    @Override
    public void onDonePressed() {
        // Do something when users tap on Done button.
    }

    @Override
    public void onSlideChanged() {

    }
}