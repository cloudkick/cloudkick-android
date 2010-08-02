package com.cloudkick;

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
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cloudkick.exceptions.BadCredentialsException;
import com.cloudkick.exceptions.EmptyCredentialsException;
import com.cloudkick.monitoring.Check;

public class NodeViewActivity extends Activity {
	private static final String TAG = "NodeViewActivity";
	private static final int SETTINGS_ACTIVITY_ID = 0;
	private static final int LOGIN_ACTIVITY_ID = 1;
	private Node node;
	private CloudkickAPI api;
	private ProgressDialog progress = null;
	private static boolean isRunning;
	private final Handler reloadHandler = new Handler();
	private final int refreshRate = 60;
	private final int metricRefreshRate = 20;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle data = this.getIntent().getExtras();
		node = (Node) data.getSerializable("node");

		RelativeLayout nodeView;
		String inflater = Context.LAYOUT_INFLATER_SERVICE;
		LayoutInflater li = (LayoutInflater) getSystemService(inflater);

		nodeView = new RelativeLayout(this);
		li.inflate(R.layout.node_view, nodeView, true);
		setContentView(nodeView);

		// If the name of the node changes we can't exactly refresh it anyway
		setTitle("Node: " + node.name);
		redraw();
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
		diskCheckService.run();
		cpuCheckService.run();
		memCheckService.run();
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

	private void redraw() {
		// Set the a color representing the state
		((TextView) findViewById(R.id.node_detail_status))
				.setBackgroundDrawable(new ColorDrawable(node.getStateColor()));

		// Set the background
		((RelativeLayout) findViewById(R.id.node_detail_header))
				.setBackgroundDrawable(new ColorDrawable(node.color));

		// Fill in the views
		((TextView) findViewById(R.id.node_detail_name)).setText(node.name);
		((TextView) findViewById(R.id.node_detail_tags)).setText(node.getTagString());
		((TextView) findViewById(R.id.value_ip_addr)).setText(node.ipAddress);
		((TextView) findViewById(R.id.value_provider)).setText(node.providerName);
		((TextView) findViewById(R.id.value_status)).setText(node.status);
		((TextView) findViewById(R.id.value_agent)).setText(node.agentState);
	}

	private class NodeUpdater extends AsyncTask<Void, Void, Node> {
		@Override
		protected Node doInBackground(Void...voids) {
			try {
				return api.getNode(node.name);
			}
			catch (BadCredentialsException e) {
				return null;
			}
		}

		@Override
		protected void onPostExecute(Node retrieved_node) {
			Log.i(TAG, "Node retrieved");
			if (progress != null) {
				progress.dismiss();
				progress = null;
			}
			if (isRunning) {
				if (retrieved_node != null) {
					node = retrieved_node;
					redraw();
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
		@Override
		protected Check doInBackground(String...checks) {
			checkName = checks[0];
			try {
				return api.getCheck(node.id, checkName);
			}
			catch (BadCredentialsException e) {
				return null;
			}
		}

		@Override
		protected void onPostExecute(Check retrieved_check) {
			Log.i(TAG, "Check Retrieved: " + checkName);
			if (isRunning && retrieved_check != null) {
				if (checkName == "disk") {
					((TextView) findViewById(R.id.value_disk))
						.setText(retrieved_check.status);
					reloadHandler.postDelayed(diskCheckService, metricRefreshRate * 1000);
				}
				else if (checkName == "cpu") {
					((TextView) findViewById(R.id.value_cpu))
						.setText(retrieved_check.status);
					reloadHandler.postDelayed(cpuCheckService, metricRefreshRate * 1000);
				}
				else if (checkName == "mem") {
					((TextView) findViewById(R.id.value_mem))
						.setText(retrieved_check.status);
					reloadHandler.postDelayed(memCheckService, metricRefreshRate * 1000);
				}
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
}