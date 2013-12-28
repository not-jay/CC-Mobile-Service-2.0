package com.xtouchme.ccmobile;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.xtouchme.ccmobile.receivers.MessageHandler;

public class Settings extends PreferenceActivity {
	
	public static final String KEY_CHECKPOINT = "checkpoint";
	public static final String KEY_WAIT_QUEUE = "wait_queue";
	
	public class DurationLabel {
		public static final String FIFTEEN_SECONDS			= "15 seconds";
		public static final String THIRTY_SECONDS			= "30 seconds";
		public static final String A_MINUTE					= "1 minute";
		public static final String TWO_MINUTES_AND_A_HALF	= "2 minutes and 30 seconds";
		public static final String FIVE_MINUTES				= "5 minutes";
		public static final String TEN_MINUTES				= "10 minutes";
	}
	public class DurationValues {
		public static final String FIFTEEN_SECONDS			= "15000";
		public static final String THIRTY_SECONDS			= "30000";
		public static final String A_MINUTE					= "60000";
		public static final String TWO_MINUTES_AND_A_HALF	= "150000";
		public static final String FIVE_MINUTES				= "300000";
		public static final String TEN_MINUTES				= "600000";
	}
	
	private SharedPreferences.OnSharedPreferenceChangeListener listener;

	@Override
	@SuppressWarnings("deprecation")
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        PreferenceManager prefMgr = getPreferenceManager();
        prefMgr.setSharedPreferencesName(MessageHandler.PREFERENCES);
		
		addPreferencesFromResource(R.xml.settings);
		
		final SharedPreferences sharedPreferences = prefMgr.getSharedPreferences();
		final Runnable r = new Runnable() {
			public void run() {
				Preference checkpoint = findPreference(KEY_CHECKPOINT);
				Preference wait_queue = findPreference(KEY_WAIT_QUEUE);
				
				String value = sharedPreferences.getString(KEY_CHECKPOINT, "Not set (default: blank)");
				checkpoint.setSummary(value);
				
				value = sharedPreferences.getString(KEY_WAIT_QUEUE, DurationValues.TWO_MINUTES_AND_A_HALF);
				if(value.equals(DurationValues.FIFTEEN_SECONDS)) value = DurationLabel.FIFTEEN_SECONDS;
				if(value.equals(DurationValues.THIRTY_SECONDS)) value = DurationLabel.THIRTY_SECONDS;
				if(value.equals(DurationValues.A_MINUTE)) value = DurationLabel.A_MINUTE;
				if(value.equals(DurationValues.TWO_MINUTES_AND_A_HALF)) value = DurationLabel.TWO_MINUTES_AND_A_HALF;
				if(value.equals(DurationValues.FIVE_MINUTES)) value = DurationLabel.FIVE_MINUTES;
				if(value.equals(DurationValues.TEN_MINUTES)) value = DurationLabel.TEN_MINUTES;
				wait_queue.setSummary(value);
			}
		};
		r.run();
		
		SharedPreferences prefs = prefMgr.getSharedPreferences();
		listener = new OnSharedPreferenceChangeListener() {
			public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
				r.run();
				Toast.makeText(Settings.this, String.format("'%s' has been changed", key), Toast.LENGTH_SHORT).show();
			}
		};
		prefs.registerOnSharedPreferenceChangeListener(listener);
	}
	
	@Override
	@SuppressWarnings("deprecation")
	protected void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(listener);
	}

	@Override
	@SuppressWarnings("deprecation")
	protected void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(listener);
	}

}
