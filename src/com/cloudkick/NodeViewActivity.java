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
import java.util.Collections;

import org.apache.http.auth.InvalidCredentialsException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cloudkick.exceptions.EmptyCredentialsException;
import com.cloudkick.monitoring.Check;

public class NodeViewActivity extends Activity {
	private static final String TAG = "NodeViewActivity";
	private static final int SETTINGS_ACTIVITY_ID = 0;
	private static final int LOGIN_ACTIVITY_ID = 1;
	private Node node;
	private RelativeLayout nodeView;
	private CloudkickAPI api;
	private ProgressDialog progress = null;
	private static boolean isRunning;
	private final Handler reloadHandler = new Handler();
	private final int nodeRefreshRate = 60;
	private final int checkRefreshRate = 45;
	private CheckAdapter adapter;
	private ArrayList<CKListItem> checks;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		NodeViewState previousState = (NodeViewState) getLastNonConfigurationInstance();
		if (previousState != null) {
			node = previousState.node;
			checks = previousState.checks;
		}
		else {
			Bundle data = this.getIntent().getExtras();
			checks = new ArrayList<CKListItem>();
			checks.add(new CKListLoadingSpinner());
			node = (Node) data.getSerializable("node");
		}

		String inflater = Context.LAYOUT_INFLATER_SERVICE;
		LayoutInflater li = (LayoutInflater) getSystemService(inflater);

		nodeView = new RelativeLayout(this);
		li.inflate(R.layout.node_view, nodeView, true);
		setContentView(nodeView);
		adapter = new CheckAdapter(this, checks);
		((ListView) findViewById(R.id.node_detail_list)).setAdapter(adapter);
		((ListView) findViewById(R.id.node_detail_list)).setOnItemClickListener(checkClickListener);
		redrawHeader();

		// If the name of the node changes we can't exactly refresh it anyway
		setTitle("Node: " + node.name);
		((ImageView) findViewById(R.id.node_detail_separator)).bringToFront();
		reloadAPI();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.node_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.do_connectbot:
				String uri = "ssh://root@" + node.ipAddress + ":22/#root";
				Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
				try {
					startActivity(i);
				}
				catch(Exception e) {
					Toast.makeText(this, "Unable to SSH to Host", Toast.LENGTH_LONG);
				}
				return true;
			default:
				// If its not recognized, do nothing
				return super.onOptionsItemSelected(item);
		}
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
		nodeReloadService.run();
		checkReloadService.run();
		Log.i(TAG, "Refresh services started");
	}

	@Override
	public void onPause() {
		super.onPause();
		isRunning = false;
		reloadHandler.removeCallbacks(nodeReloadService);
		reloadHandler.removeCallbacks(checkReloadService);
		Log.i(TAG, "Reloading callbacks canceled");
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return new NodeViewState(node, checks);
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

	private void redrawHeader() {
		// Set the a color representing the state
		((TextView) findViewById(R.id.node_detail_status))
			.setBackgroundDrawable(new ColorDrawable(node.getStateColor()));

		// Set the background
		((RelativeLayout) findViewById(R.id.node_detail_header))
			.setBackgroundDrawable(new ColorDrawable(node.color));

		((TextView) findViewById(R.id.node_detail_name)).setText(node.name);
		((TextView) findViewById(R.id.node_detail_tags)).setText(node.getTagString());
	}

	private class NodeUpdater extends AsyncTask<Void, Void, Node> {
		private Exception e = null;

		@Override
		protected Node doInBackground(Void...voids) {
			try {
				return api.getNode(node.name);
			}
			catch (Exception e) {
				this.e = e;
				return null;
			}
		}

		@Override
		protected void onPostExecute(Node retrieved_node) {
			Log.i(TAG, "Node retrieved");
			// Get rid of the progress dialog
			if (progress != null) {
				progress.dismiss();
				progress = null;
			}
			// Handle Error
			if (e != null) {
				if (e instanceof InvalidCredentialsException) {
					Toast.makeText(NodeViewActivity.this.getApplicationContext(), "Invalid Credentials", Toast.LENGTH_SHORT).show();
					Intent settingsActivity = new Intent(getBaseContext(), Preferences.class);
					startActivityForResult(settingsActivity, SETTINGS_ACTIVITY_ID);
				}
				else if (e instanceof IOException) {
					Toast.makeText(NodeViewActivity.this.getApplicationContext(), "A Network Error Occurred", Toast.LENGTH_SHORT).show();
					Log.e(TAG, "Network Error", e);
				}
				else {
					Toast.makeText(NodeViewActivity.this.getApplicationContext(), "Unknown Refresh Error", Toast.LENGTH_SHORT).show();
					Log.e(TAG, "Unknown Refresh Error", e);
				}
			}
			// Handle success
			else if (isRunning) {
				if (retrieved_node != null) {
					node = retrieved_node;
					//fillNodeDetails();
					adapter.notifyDataSetChanged();
					((TextView) findViewById(R.id.node_detail_tags)).setText(node.getTagString());
					// Schedule the next run
					reloadHandler.postDelayed(nodeReloadService, nodeRefreshRate * 1000);
					Log.i(TAG, "Next reload in " + nodeRefreshRate + " seconds");
				} else {
					Toast.makeText(NodeViewActivity.this.getApplicationContext(), "Invalid Credentials", Toast.LENGTH_SHORT).show();
					Intent settingsActivity = new Intent(getBaseContext(), Preferences.class);
					startActivityForResult(settingsActivity, SETTINGS_ACTIVITY_ID);
				}
			}
		}
	}

	private class CheckUpdater extends AsyncTask<Void, Void, ArrayList<Check>> {
		private Exception e = null;

		@Override
		protected ArrayList<Check> doInBackground(Void...voids) {
			try {
				return api.getChecks(node.id);
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
					Toast.makeText(NodeViewActivity.this.getApplicationContext(), "Invalid Credentials", Toast.LENGTH_SHORT).show();
					Intent settingsActivity = new Intent(getBaseContext(), Preferences.class);
					startActivityForResult(settingsActivity, SETTINGS_ACTIVITY_ID);
				}
				else if (e instanceof IOException) {
					Toast.makeText(NodeViewActivity.this.getApplicationContext(), "A Network Error Occurred", Toast.LENGTH_SHORT).show();
					Log.e(TAG, "Network Error", e);
				}
				else {
					Toast.makeText(NodeViewActivity.this.getApplicationContext(), "Unknown Refresh Error", Toast.LENGTH_SHORT).show();
					Log.e(TAG, "Unknown Refresh Error", e);
				}
			}
			// Handle success
			else if (isRunning) {
				checks.clear();
				checks.addAll(retrievedChecks);
				Collections.sort(checks);
				adapter.notifyDataSetChanged();
				// Schedule the next run
				reloadHandler.postDelayed(checkReloadService, checkRefreshRate * 1000);
				Log.i(TAG, "Next check reload in " + checkRefreshRate + " seconds");
			}
		}
	}

	private final Runnable nodeReloadService = new Runnable() {
		public void run() {
			// These happen asynchronously and schedules their own next runs
			if (api != null) {
				new NodeUpdater().execute();
			}
		}
	};

	private final Runnable checkReloadService = new Runnable() {
		public void run() {
			if (api != null) {
				new CheckUpdater().execute();
			}
		}
	};


	public class CheckAdapter extends ArrayAdapter<CKListItem> {

		public CheckAdapter(Context context, ArrayList<CKListItem> items)
		{
			super(context, -1, items);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			CKListItem item = getItem(position);
			return item.getItemView(getContext(), convertView, parent);
		}

		@Override
		public int getViewTypeCount() {
			return 2;
		}

		@Override
		public int getItemViewType(int position) {
			CKListItem item = getItem(position);
			if (item instanceof Check) {
				return 0;
			}
			else {
				return 1;
			}
		}
	}

	private class NodeViewState {
		public final Node node;
		public final ArrayList<CKListItem> checks;

		public NodeViewState(Node node, ArrayList<CKListItem> checks) {
			this.node = node;
			this.checks = checks;
		}
	}

	private OnItemClickListener checkClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			CKListItem item = checks.get(position);
			if (item instanceof Check) {
				// Build the bundle
				Bundle data = new Bundle();
				data.putString("nodeName", node.name);
				data.putString("nodeId", node.id);
				data.putString("checkId", ((Check) item).id);
				data.putSerializable("check", (item));

				// Start a CheckViewActivity
				Intent intent = new Intent(NodeViewActivity.this, CheckViewActivity.class);
				intent.putExtras(data);
				startActivity(intent);
			}
		}
	};
}
