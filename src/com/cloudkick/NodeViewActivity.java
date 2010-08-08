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
import java.text.DecimalFormat;

import org.apache.http.auth.InvalidCredentialsException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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
	private final int refreshRate = 60;
	private final int metricRefreshRate = 20;
	private final DecimalFormat metricFormat = new DecimalFormat("0.##");
	private NodeDetailAdapter adapter;
	private NodeDetailItem[] details;

	// Java's enum's seem utterly worthless, am I missing something?
	private final int CPU = 0;
	private final int MEM = 1;
	private final int DISK = 2;
	private final int IP = 3;
	private final int PROVIDER = 4;
	private final int STATUS = 5;
	private final int AGENT = 6;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		NodeViewState previousState = (NodeViewState) getLastNonConfigurationInstance();
		if (previousState != null) {
			node = previousState.node;
			details = previousState.details;
		}
		else {
			Bundle data = this.getIntent().getExtras();
			node = (Node) data.getSerializable("node");
			details = new NodeDetailItem[7];
			details[CPU] = new NodeDetailItem("CPU Use", "Loading...", null);
			details[MEM] = new NodeDetailItem("Memory Use", "Loading...", null);
			details[DISK] = new NodeDetailItem("Disk Use", "Loading...", null);
			fillNodeDetails();
		}

		String inflater = Context.LAYOUT_INFLATER_SERVICE;
		LayoutInflater li = (LayoutInflater) getSystemService(inflater);

		nodeView = new RelativeLayout(this);
		li.inflate(R.layout.node_view, nodeView, true);
		setContentView(nodeView);
		adapter = new NodeDetailAdapter(this, R.layout.node_detail, details);
		((ListView) findViewById(R.id.node_detail_list)).setAdapter(adapter);
		redrawHeader();

		// If the name of the node changes we can't exactly refresh it anyway
		setTitle("Node: " + node.name);

		reloadAPI();
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
		reloadService.run();
		if (node.agentState.equals("connected")) {
			diskCheckService.run();
			cpuCheckService.run();
			memCheckService.run();
		}
		else {
			details[CPU].value = "Agent Not Connected";
			details[MEM].value = "Agent Not Connected";
			details[DISK].value = "Agent Not Connected";
			adapter.notifyDataSetChanged();
		}

		Log.i(TAG, "Refresh services started");
	}

	@Override
	public void onPause() {
		super.onPause();
		isRunning = false;
		reloadHandler.removeCallbacks(reloadService);
		reloadHandler.removeCallbacks(diskCheckService);
		reloadHandler.removeCallbacks(cpuCheckService);
		reloadHandler.removeCallbacks(memCheckService);
		Log.i(TAG, "Reloading callbacks canceled");
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return new NodeViewState(node, details);
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

	private void fillNodeDetails() {
		details[IP] = new NodeDetailItem("IP Address", node.ipAddress, null);
		details[PROVIDER] = new NodeDetailItem("Provider", node.providerName, null);
		details[STATUS] = new NodeDetailItem("Status", node.status, null);
		details[AGENT] = new NodeDetailItem("Agent", node.agentState, null);
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
					fillNodeDetails();
					adapter.notifyDataSetChanged();
					((TextView) findViewById(R.id.node_detail_tags)).setText(node.getTagString());
					// Schedule the next run
					reloadHandler.postDelayed(reloadService, refreshRate * 1000);
					Log.i(TAG, "Next reload in " + refreshRate + " seconds");
				} else {
					Toast.makeText(NodeViewActivity.this.getApplicationContext(), "Invalid Credentials", Toast.LENGTH_SHORT).show();
					Intent settingsActivity = new Intent(getBaseContext(), Preferences.class);
					startActivityForResult(settingsActivity, SETTINGS_ACTIVITY_ID);
				}
			}
		}
	}

	private class MetricUpdater extends AsyncTask<String, Void, Check> {
		private String checkName;
		private Exception e = null;

		@Override
		protected Check doInBackground(String...checks) {
			checkName = checks[0];
			try {
				return api.getCheck(node.id, checkName);
			}
			catch (Exception e) {
				this.e = e;
				return null;
			}
		}

		@Override
		protected void onPostExecute(Check retrieved_check) {
			Log.i(TAG, "Check Retrieved: " + checkName);
			// Handle error
			if (e != null) {
				if (e instanceof InvalidCredentialsException) {
					Toast.makeText(NodeViewActivity.this.getApplicationContext(), "Invalid Credentials", Toast.LENGTH_SHORT).show();
					Intent settingsActivity = new Intent(getBaseContext(), Preferences.class);
					startActivityForResult(settingsActivity, SETTINGS_ACTIVITY_ID);
				}
				else if (e instanceof IOException) {
					Toast.makeText(NodeViewActivity.this.getApplicationContext(), "A Network Error Occurred", Toast.LENGTH_SHORT).show();
				}
				else {
					Toast.makeText(NodeViewActivity.this.getApplicationContext(), "Unknown Refresh Error", Toast.LENGTH_SHORT).show();
					Log.e(TAG, "Unknown Refresh Error", e);
				}
			}
			// Handle success
			else if (isRunning) {
				if (checkName.equals("disk")) {
					try {
						Float blocks = retrieved_check.metrics.get("blocks");
						Float bfree = retrieved_check.metrics.get("bfree");
						Float percentage = (1 - (bfree / blocks)) * 100;
						Float mbUsed = ((blocks - bfree) * 4096) / (1024 * 1024);
						details[DISK].value =
							metricFormat.format(percentage) + "%, " + metricFormat.format(mbUsed) + " MB used";
					}
					catch (NullPointerException e) {
						details[DISK].value = "Load Error";
					}
					reloadHandler.postDelayed(diskCheckService, metricRefreshRate * 1000);
				}
				else if (checkName.equals("cpu")) {
					try {
						Float percentage = 100 - retrieved_check.metrics.get("cpu_idle");
						details[CPU].value =
							metricFormat.format(percentage) + "%";
					}
					catch (NullPointerException e) {
						details[CPU].value = "Load Error";
					}
					reloadHandler.postDelayed(cpuCheckService, metricRefreshRate * 1000);
				}
				else if (checkName.equals("mem")) {
					try {
						Float memTotal = retrieved_check.metrics.get("mem_total");
						Float memUsed = retrieved_check.metrics.get("mem_used");
						Float mbUsed = (memUsed / (1024 * 1024));
						Float percentage = (memUsed / memTotal) * 100;
						details[MEM].value =
							metricFormat.format(percentage) + "%, " + metricFormat.format(mbUsed) + " MB used";
					}
					catch (NullPointerException e) {
						details[MEM].value = "Load Error";
					}
					reloadHandler.postDelayed(memCheckService, metricRefreshRate * 1000);
				}
				adapter.notifyDataSetChanged();
				// Schedule the next run
				Log.i(TAG, "Next " + checkName + " reload in " + metricRefreshRate + " seconds");
			}
		}
	}

	private final Runnable reloadService = new Runnable() {
		public void run() {
			// These happen asynchronously and schedules their own next runs
			if (api != null) {
				new NodeUpdater().execute();
			}
		}
	};

	// TODO: Reduce code duplication in here

	private final Runnable diskCheckService = new Runnable() {
		public void run() {
			if (api != null) {
				new MetricUpdater().execute("disk");
			}
		}
	};

	private final Runnable cpuCheckService = new Runnable() {
		public void run() {
			if (api != null) {
				new MetricUpdater().execute("cpu");
			}
		}
	};

	private final Runnable memCheckService = new Runnable() {
		public void run() {
			if (api != null) {
				new MetricUpdater().execute("mem");
			}
		}
	};

	private class NodeViewState {
		public final Node node;
		public final NodeDetailItem[] details;

		public NodeViewState(Node node, NodeDetailItem[] details) {
			this.node = node;
			this.details = details;
		}
	}

	public class NodeDetailAdapter extends ArrayAdapter<NodeDetailItem> {
		private final int resource;

		public NodeDetailAdapter(Context context, int resource, NodeDetailItem[] items)
		{
			super(context, resource, items);
			this.resource = resource;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			RelativeLayout detailView;
			NodeDetailItem item = getItem(position);

			String inflater = Context.LAYOUT_INFLATER_SERVICE;
			LayoutInflater li = (LayoutInflater)getContext().getSystemService(inflater);

			if(convertView == null) {
				detailView = new RelativeLayout(getContext());
				li.inflate(resource, detailView, true);
			}

			else {
				detailView = (RelativeLayout) convertView;
			}

			((TextView) detailView.findViewById(R.id.detail_label)).setText(item.label);
			((TextView) detailView.findViewById(R.id.detail_value)).setText(item.value);

			return detailView;
		}
	}

	private class NodeDetailItem {
		public String label;
		public String value;
		public Float percentage;

		public NodeDetailItem(String label, String value, Float percentage) {
			this.label = label;
			this.value = value;
			this.percentage = percentage;
		}
	}
}
