package com.chariotinstruments.chariotgauge;


import java.util.prefs.Preferences;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.view.View;

public class SettingsActivity extends PreferenceActivity {
	
	View root;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);
        getPreferenceManager()
        .findPreference("go_to_site")
        .setOnPreferenceClickListener(
           new OnPreferenceClickListener() {
        	 public boolean onPreferenceClick(Preference preference) {
             Intent intent = new Intent(Intent.ACTION_VIEW);
             intent.setData(Uri.parse("https://www.chariotgauge.com/product/controller/"));
             startActivity(intent);
             return true;
         }
     });
	}
}
