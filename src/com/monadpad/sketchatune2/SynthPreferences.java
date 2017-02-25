package com.monadpad.sketchatune2;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.view.View;

public class SynthPreferences extends PreferenceFragment {
//public class SynthPreferences extends ListFragment {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.synth_prefs);
	}

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference){

        final View viewmpad = SynthPreferences.this.getActivity().findViewById(R.id.mpad);
        if (viewmpad != null){

            if (preference.getClass().equals(CheckBoxPreference.class)){

                ((MainActivity)SynthPreferences.this.getActivity()).
                        refreshPreference(preference);

            }
            else {

                ((MonadView)viewmpad).maybeUpdateTutorial();

                preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object o) {


                        ((MainActivity)SynthPreferences.this.getActivity()).
                            refreshPreference(preference, o);

                        return true;
                    }
                });
            }

        }
        return true;
    }
}

