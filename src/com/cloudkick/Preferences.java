package com.cloudkick;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

public class Preferences extends PreferenceActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String key = prefs.getString("editKey", "");
		String secret = prefs.getString("editSecret", "");
		if (key == "" && secret == "") {
			LayoutInflater i = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
			View content = i.inflate(R.layout.welcome, null);
			AlertDialog.Builder dialog = new AlertDialog.Builder(this);
			dialog.setView(content)
				  .setPositiveButton("OK", new DialogInterface.OnClickListener() {
					  public void onClick(DialogInterface dialog, int id) {
						  dialog.cancel();
					  }
				  });
			dialog.show();
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String key = prefs.getString("editKey", "");
		String secret = prefs.getString("editSecret", "");
		if ((key == "" || secret == "") && keyCode == KeyEvent.KEYCODE_BACK) {
			Toast.makeText(this, "You Must Enter a Cloudkick API Key and Secret", Toast.LENGTH_LONG).show();
			return true;
		}
		else {
			return super.onKeyDown(keyCode, event);
		}
	}
}