package com.cloudkick;

import java.util.ArrayList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.cloudkick.exceptions.BadCredentialsException;
import com.cloudkick.exceptions.CredentialsException;

public class DashboardActivity extends Activity implements OnItemClickListener {
	private static final String TAG = "DashboardActivity";
	private static final int SETTINGS_ACTIVITY_ID = 0;
	private CloudkickAPI api;
	private ProgressDialog progress;
	private ListView dashboard;
	private NodesAdapter adapter;
	private final ArrayList<Node> nodes = new ArrayList<Node>();
	private SharedPreferences prefs;
	private final Time lastRefresh = new Time();

	private void refreshNodes() {
		lastRefresh.setToNow();
		new NodeUpdater().execute();
	}

	private void reloadAPI() {
		lastRefresh.setToNow();
	    try {
		    progress = ProgressDialog.show(this, "", "Loading Nodes...", true);
	    	api = new CloudkickAPI(this);
	    	refreshNodes();
	    }
	    catch (CredentialsException e) {
			progress.dismiss();
			Intent settingsActivity = new Intent(getBaseContext(), Preferences.class);
	    	startActivityForResult(settingsActivity, SETTINGS_ACTIVITY_ID);
	    }
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
	    super.onCreate(savedInstanceState);
	    PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
	    prefs = PreferenceManager.getDefaultSharedPreferences(this);

	    dashboard = new ListView(this);
	    adapter = new NodesAdapter(this, R.layout.node_item, nodes);
		dashboard.setAdapter(adapter);
		dashboard.setOnItemClickListener(this);
		dashboard.setBackgroundColor(Color.WHITE);

	    setContentView(dashboard);
	    reloadAPI();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.dashboard_menu, menu);
	    return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
		    case R.id.refresh_dashboard:
		    	refreshNodes();
		        return true;
		    case R.id.settings:
		    	Intent settingsActivity = new Intent(getBaseContext(), Preferences.class);
		    	startActivityForResult(settingsActivity, SETTINGS_ACTIVITY_ID);
		    	return true;
		    default:
		        return super.onOptionsItemSelected(item);
	    }
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == SETTINGS_ACTIVITY_ID) {
			reloadAPI();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		Time now = new Time();
		now.setToNow();
		long delta = (now.toMillis(true) - lastRefresh.toMillis(true))/60000;
		long maxDelta = new Long(prefs.getString("refreshDelta", "5"));

		if (delta >= maxDelta) {
			refreshNodes();
		}
	}

	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Bundle data = new Bundle();
		data.putSerializable("node", nodes.get(position));
		Intent intent = new Intent(DashboardActivity.this, NodeViewActivity.class);
		intent.putExtras(data);
		startActivity(intent);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	  super.onConfigurationChanged(newConfig);
	  setContentView(dashboard);
	}

	private class NodeUpdater extends AsyncTask<Void, Void, ArrayList<Node>> {
		private Exception error = null;
		@Override
		protected ArrayList<Node> doInBackground(Void...voids) {
			lastRefresh.setToNow();
			try {
				return api.getNodes();
			}
			catch (BadCredentialsException e) {
				error = e;
				return null;
			}
		}

		@Override
		protected void onPostExecute(ArrayList<Node> retrieved_nodes) {
			if (retrieved_nodes != null) {
				Log.i(TAG, "Retrieved " + retrieved_nodes.size() + " Nodes");
				nodes.clear();
				nodes.addAll(retrieved_nodes);
				adapter.notifyDataSetChanged();
				if (progress != null) {
					progress.dismiss();
					progress = null;
				}
				else {
					Toast.makeText(DashboardActivity.this.getApplicationContext(), "Dashboard Refreshed", Toast.LENGTH_SHORT).show();
				}
			} else {
				if (progress != null) {
					progress.dismiss();
					progress = null;
				}
				Toast.makeText(DashboardActivity.this.getApplicationContext(), "Invalid Credentials", Toast.LENGTH_SHORT).show();
				Intent settingsActivity = new Intent(getBaseContext(), Preferences.class);
		    	startActivityForResult(settingsActivity, SETTINGS_ACTIVITY_ID);
			}
		}
	}
}
