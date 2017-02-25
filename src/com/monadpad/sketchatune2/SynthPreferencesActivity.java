package com.monadpad.sketchatune2;

import android.os.Bundle;
import android.preference.PreferenceActivity;
//import com.monadpad.fingergrooves.R;

public class SynthPreferencesActivity extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.synth_prefs);
    }
}
