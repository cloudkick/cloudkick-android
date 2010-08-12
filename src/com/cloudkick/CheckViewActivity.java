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

import java.io.IOException;
import java.util.ArrayList;

import org.apache.http.auth.InvalidCredentialsException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cloudkick.exceptions.EmptyCredentialsException;
import com.cloudkick.monitoring.Check;

public class CheckViewActivity extends Activity {
	private static final String TAG = "CheckViewActivity";
	private static final int SETTINGS_ACTIVITY_ID = 0;
	private static final int LOGIN_ACTIVITY_ID = 1;
	private boolean isRunning = false;
	private final Handler reloadHandler = new Handler();
	private CloudkickAPI api = null;
	private final int checkRefreshRate = 30;
	private RelativeLayout checkView;
	private String nodeName;
	private String nodeId;
	private String checkId;
	private Check check;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		CheckViewState previousState = (CheckViewState) getLastNonConfigurationInstance();
		if (previousState != null) {
			nodeName = previousState.nodeName;
			nodeId = previousState.nodeId;
			checkId = previousState.checkId;
			check = previousState.check;
		}
		else {
			Bundle data = this.getIntent().getExtras();
			nodeName = (String) data.getSerializable("nodeName");
			nodeId = (String) data.getSerializable("nodeId");
			checkId = (String) data.getSerializable("checkId");
			check = (Check) data.getSerializable("check");
		}

		String inflater = Context.LAYOUT_INFLATER_SERVICE;
		LayoutInflater li = (LayoutInflater) getSystemService(inflater);

		checkView = new RelativeLayout(this);
		li.inflate(R.layout.check_view, checkView, true);
		setContentView(checkView);

		setTitle(nodeName + ": " + check.type + " check");
		redrawCheck();
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return new CheckViewState(nodeName, nodeId, checkId, check);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == SETTINGS_ACTIVITY_ID || requestCode == LOGIN_ACTIVITY_ID) {
			reloadAPI();
		}
	}

	@Override
	public void onResume(){
		super.onResume();
		isRunning = true;
		checkReloadService.run();
		Log.i(TAG, "Refresh services started");
	}

	@Override
	public void onPause() {
		super.onPause();
		isRunning = false;
		reloadHandler.removeCallbacks(checkReloadService);
		Log.i(TAG, "Reloading callbacks canceled");
	}

	private void reloadAPI() {
		try {
			api = new CloudkickAPI(this);
		}
		catch (EmptyCredentialsException e) {
			Log.i(TAG, "Empty Credentials, forcing login");
			Intent loginActivity = new Intent(getBaseContext(), LoginActivity.class);
			startActivityForResult(loginActivity, LOGIN_ACTIVITY_ID);
		}
	}

	private void redrawCheck() {
		((TextView) findViewById(R.id.check_detail_name))
			.setText(nodeName + " " + check.type + " Check");

		((TextView) findViewById(R.id.check_detail_current_label))
			.setText("Latest State (" + check.latestState.whence + ")");
		((TextView) findViewById(R.id.check_detail_current_state))
			.setText(check.latestState.status);

		((TextView) findViewById(R.id.check_detail_previous_label))
		.setText("Previous State (" + check.previousState.whence + ")");
		((TextView) findViewById(R.id.check_detail_previous_state))
			.setText(check.previousState.status);
	}

	private class CheckUpdater extends AsyncTask<Void, Void, ArrayList<Check>> {
		private Exception e = null;

		@Override
		protected ArrayList<Check> doInBackground(Void...voids) {
			try {
				return api.getChecks(nodeId);
			}
			catch (Exception e) {
				this.e = e;
				return null;
			}
		}

		@Override
		protected void onPostExecute(ArrayList<Check> retrievedChecks) {
			// Handle error
			if (e != null) {
				if (e instanceof InvalidCredentialsException) {
					Toast.makeText(CheckViewActivity.this.getApplicationContext(), "Invalid Credentials", Toast.LENGTH_SHORT).show();
					Intent settingsActivity = new Intent(getBaseContext(), Preferences.class);
					startActivityForResult(settingsActivity, SETTINGS_ACTIVITY_ID);
				}
				else if (e instanceof IOException) {
					Toast.makeText(CheckViewActivity.this.getApplicationContext(), "A Network Error Occurred", Toast.LENGTH_SHORT).show();
					Log.e(TAG, "Network Error", e);
				}
				else {
					Toast.makeText(CheckViewActivity.this.getApplicationContext(), "Unknown Refresh Error", Toast.LENGTH_SHORT).show();
					Log.e(TAG, "Unknown Refresh Error", e);
				}
			}
			// Handle success
			else if (isRunning) {
				redrawCheck();
				// Schedule the next run
				reloadHandler.postDelayed(checkReloadService, checkRefreshRate * 1000);
				Log.i(TAG, "Next check reload in " + checkRefreshRate + " seconds");
			}
		}
	}

	private class CheckViewState {
		private final String nodeName;
		private final String nodeId;
		private final String checkId;
		private final Check check;

		public CheckViewState(String nodeName, String nodeId, String checkId, Check check) {
			this.nodeName = nodeName;
			this.nodeId = nodeId;
			this.checkId = checkId;
			this.check = check;
		}
	}

	private final Runnable checkReloadService = new Runnable() {
		public void run() {
			if (api != null) {
				new CheckUpdater().execute();
			}
		}
	};
}
