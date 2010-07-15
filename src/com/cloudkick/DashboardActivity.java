package com.cloudkick;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;

public class DashboardActivity extends Activity {
	private CloudkickAPI api;
	private ProgressDialog progress;
	private ListView dashboard;
	private NodesAdapter adapter;
	private Node[] nodes = new Node[0];
	
	private void refreshNodes()
	{
	    progress = ProgressDialog.show(this, "", "Retrieving Nodes...", true);
		new NodeUpdater().execute();
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
	    super.onCreate(savedInstanceState);
        // You have to hard code your keys for now. This is bad.
	    api = new CloudkickAPI("", "");
	    
	    dashboard = new ListView(this);
	    adapter = new NodesAdapter(this, R.layout.node_item, nodes);
		dashboard.setAdapter(adapter);
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
		    default:
		        return super.onOptionsItemSelected(item);
	    }
	}
	
	private class NodeUpdater extends AsyncTask<Void, Void, Node[]> {
		protected Node[] doInBackground(Void...voids) {
			return api.getNodes();
		}
		
		protected void onPostExecute(Node[] retrieved_nodes) {
		    adapter = new NodesAdapter(DashboardActivity.this, R.layout.node_item, retrieved_nodes);
			dashboard.setAdapter(adapter);
			adapter.notifyDataSetChanged();
			progress.dismiss();
		}
	}
}
