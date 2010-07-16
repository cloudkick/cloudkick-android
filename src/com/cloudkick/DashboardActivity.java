package com.cloudkick;

import org.json.JSONException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;

public class DashboardActivity extends Activity {
	private static final String TAG = "DashboardActivity";
	private CloudkickAPI api;
	private ProgressDialog progress;
	private ListView dashboard;
	private NodesAdapter adapter;
	private Node[] nodes = new Node[0];
	private SharedPreferences prefs;
	
	private void refreshNodes()
	{
	    progress = ProgressDialog.show(this, "", "Retrieving Nodes...", true);
		new NodeUpdater().execute();
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
	    super.onCreate(savedInstanceState);
	    PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
	    prefs = PreferenceManager.getDefaultSharedPreferences(this);
	    api = new CloudkickAPI(prefs.getString("editKey", ""), prefs.getString("editSecret", ""));
	    
	    dashboard = new ListView(this);
	    adapter = new NodesAdapter(this, R.layout.node_item, nodes);
		dashboard.setAdapter(adapter);
		dashboard.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Bundle data = new Bundle();
				data.putSerializable("node", nodes[position]);
				Intent intent = new Intent(DashboardActivity.this, NodeViewActivity.class);
				intent.putExtras(data);
				startActivityForResult(intent, 0);
			}
		});

	    setContentView(dashboard);
	    refreshNodes();
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
		    	startActivity(settingsActivity);
		    	return true;
		    default:
		        return super.onOptionsItemSelected(item);
	    }
	}
	
	private class NodeUpdater extends AsyncTask<Void, Void, Node[]> {
		protected Node[] doInBackground(Void...voids) {
			return api.getNodes();
		}
		
		protected void onPostExecute(Node[] retrieved_nodes) {
			try {
				Log.i(TAG, "Retrieved " + retrieved_nodes.length + " Nodes");
				adapter = new NodesAdapter(DashboardActivity.this, R.layout.node_item, retrieved_nodes);
				nodes = retrieved_nodes;
				dashboard.setAdapter(adapter);
				adapter.notifyDataSetChanged();
				progress.dismiss();
			}
			catch (Exception e) {
				progress.dismiss();
				Intent settingsActivity = new Intent(getBaseContext(), Preferences.class);
		    	startActivity(settingsActivity);
			}
		}
	}
}
