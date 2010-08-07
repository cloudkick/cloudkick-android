/*
 * Licensed to Cloudkick, Inc ('Cloudkick') under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Cloudkick licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloudkick;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class LoginActivity extends Activity {
	private static final int SETTINGS_ACTIVITY_ID = 0;
	RelativeLayout loginView = null;
	private String user = null;
	private String pass = null;
	private ProgressDialog progress = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);
		setTitle("Cloudkick for Android");

		findViewById(R.id.button_login).setOnClickListener(new LoginClickListener());
		findViewById(R.id.button_signup).setOnClickListener(new SignupClickListener());
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == SETTINGS_ACTIVITY_ID) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);
			if (prefs.getString("editKey", "").equals("") && prefs.getString("editSecret", "").equals("")) {
				finish();
			}
			else {
				Intent result = new Intent();
				result.putExtra("login", true);
				setResult(Activity.RESULT_OK, result);
				finish();
			}
		}
	}

	private class LoginClickListener implements View.OnClickListener {
		public void onClick(View v) {
			new AccountLister().execute();
		}
	}

	private class SignupClickListener implements View.OnClickListener {
		public void onClick(View v) {
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.cloudkick.com/pricing/")));
		}
	}

	private class AccountLister extends AsyncTask<Void, Void, ArrayList<String>>{
		private Integer statusCode = null;

		@Override
		protected void onPreExecute() {
			user = ((EditText) findViewById(R.id.input_email)).getText().toString();
			pass = ((EditText) findViewById(R.id.input_password)).getText().toString();
			progress = ProgressDialog.show(LoginActivity.this, "", "Logging In...", true);
		}

		@Override
		protected ArrayList<String> doInBackground(Void...voids) {
			ArrayList<String> accounts = new ArrayList<String>();
			try {
				HttpClient client = new DefaultHttpClient();
				HttpPost post = new HttpPost("https://www.cloudkick.com/oauth/list_accounts/");
				ArrayList<NameValuePair> values = new ArrayList<NameValuePair>(2);
				values.add(new BasicNameValuePair("user", user));
				values.add(new BasicNameValuePair("password", pass));
				post.setEntity(new UrlEncodedFormEntity(values));
				HttpResponse response = client.execute(post);
				statusCode = response.getStatusLine().getStatusCode();
				InputStream is = response.getEntity().getContent();
				BufferedReader rd = new BufferedReader(new InputStreamReader(is));
				String line;
				while ((line = rd.readLine()) != null) {
					accounts.add(line);
					Log.i("LoginActivity", line);
				}
			}
			catch (Exception e) {
				e.printStackTrace();
				statusCode = 0;
			}
			return accounts;
		}

		@Override
		protected void onPostExecute(ArrayList<String> accounts) {
			switch (statusCode) {
				case 200:
					if (accounts.size() == 1) {
						new KeyRetriever().execute(accounts.get(0));
					}
					else {
						String[] tmpAccountArray = new String[accounts.size()];
						final String[] accountArray = accounts.toArray(tmpAccountArray);
						AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
						builder.setTitle("Select an Account");
						builder.setItems(accountArray, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int item) {
								new KeyRetriever().execute(accountArray[item]);
							}
						});
						AlertDialog selectAccount = builder.create();
						selectAccount.show();
					}
					break;
				case 400:
					progress.dismiss();
					if (accounts.get(0).equals("You have enabled multi factor authentication for this account. To access the API key list, please visit the website.")) {
						AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
						builder.setTitle("MFA is Enabled");
						String mfaMessage = ("You appear to have multi-factor authentication enabled on your account. "
											+ "You will need to manually create an API key with read permissions in the "
											+ "web interface, then enter it directly in the settings panel.");
						builder.setMessage(mfaMessage);
						builder.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								Intent settingsActivity = new Intent(getBaseContext(), Preferences.class);
								startActivityForResult(settingsActivity, SETTINGS_ACTIVITY_ID);
							}
						});
						AlertDialog mfaDialog = builder.create();
						mfaDialog.show();
					}
					else {
						Toast.makeText(LoginActivity.this, "Invalid Username or Password", Toast.LENGTH_LONG).show();
					}
					break;
				default:
					progress.dismiss();
					Toast.makeText(LoginActivity.this, "An Error Occurred Retrieving Your Accounts", Toast.LENGTH_LONG).show();
			};
		}
	}

	private class KeyRetriever extends AsyncTask<String, Void, String[]>{
		private Integer statusCode = null;

		@Override
		protected String[] doInBackground(String...accts) {
			Log.i("LoginActivity", "Selected Account: " + accts[0]);
			String[] creds = new String[2];
			try {
				HttpClient client = new DefaultHttpClient();
				HttpPost post = new HttpPost("https://www.cloudkick.com/oauth/create_consumer/");
				ArrayList<NameValuePair> values = new ArrayList<NameValuePair>(2);
				values.add(new BasicNameValuePair("user", user));
				values.add(new BasicNameValuePair("password", pass));
				values.add(new BasicNameValuePair("account", accts[0]));
				values.add(new BasicNameValuePair("system", "Cloudkick for Android"));
				values.add(new BasicNameValuePair("perm_read", "True"));
				values.add(new BasicNameValuePair("perm_write", "False"));
				values.add(new BasicNameValuePair("perm_execute", "False"));
				post.setEntity(new UrlEncodedFormEntity(values));
				HttpResponse response = client.execute(post);
				statusCode = response.getStatusLine().getStatusCode();
				Log.i("LoginActivity", "Return Code: " + statusCode);
				InputStream is = response.getEntity().getContent();
				BufferedReader rd = new BufferedReader(new InputStreamReader(is));
				String line;
				for (int i = 0; i < 2; i++) {
					line = rd.readLine();
					if (line == null) {
						return creds;
					}
					creds[i] = line;
				}
			}
			catch (Exception e) {
				statusCode = 0;
			}
			return creds;
		}

		@Override
		protected void onPostExecute(String[] creds) {
			progress.dismiss();
			if (statusCode != 200) {
				// Show short error messages - this is a dirty hack
				if (creds[0] != null && creds[0].startsWith("User with role")) {
					Toast.makeText(LoginActivity.this, creds[0], Toast.LENGTH_LONG).show();
				}
				else {
					Toast.makeText(LoginActivity.this, "An Error Occurred on Login", Toast.LENGTH_LONG).show();
					return;
				}
			}
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString("editKey", creds[0]);
			editor.putString("editSecret", creds[1]);
			editor.commit();
			Intent result = new Intent();
			result.putExtra("login", true);
			setResult(Activity.RESULT_OK, result);
			LoginActivity.this.finish();
		}
	}
}
