package ch.ethz.inf.vs.a4.savemyass.UI;

import android.graphics.Color;
import android.os.Bundle;

import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntroFragment;

import ch.ethz.inf.vs.a4.savemyass.R;

public class Intro extends AppIntro {

    @Override
    public void init(Bundle savedInstanceState) {
        addSlide(AppIntroFragment.newInstance(getResources().getString(R.string.intro_welcome), getResources().getString(R.string.intro_welcome_text), R.raw.logohelp, getResources().getColor(R.color.accent)));
        addSlide(AppIntroFragment.newInstance(getResources().getString(R.string.intro_location), getResources().getString(R.string.intro_location_text), R.raw.geolocation, getResources().getColor(R.color.accent)));
        addSlide(AppIntroFragment.newInstance(getResources().getString(R.string.intro_p2p), getResources().getString(R.string.intro_p2p_text), R.raw.p2p, getResources().getColor(R.color.accent)));
        addSlide(AppIntroFragment.newInstance(getResources().getString(R.string.intro_security), getResources().getString(R.string.intro_security_text), R.raw.secure, getResources().getColor(R.color.accent)));

        setBarColor(getResources().getColor(R.color.primary_dark));
        setSeparatorColor(getResources().getColor(R.color.divider));

        showSkipButton(true);
        showDoneButton(true);

    }

    @Override
    public void onSkipPressed() {
        finish();
    }

    @Override
    public void onNextPressed() {

    }

    @Override
    public void onDonePressed() {
        finish();
    }

    @Override
    public void onSlideChanged() {

    }
}