package com.cloudkick;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;

public class CloudkickActivity extends TabActivity {
	private TabHost mTabHost;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.main);

	    mTabHost = getTabHost();
	    
	    mTabHost.addTab(mTabHost.newTabSpec("tab_dashboard")
	    			.setIndicator("Dashboard")
	    			.setContent(new Intent().setClass(this, DashboardActivity.class)));
	    mTabHost.addTab(mTabHost.newTabSpec("tab_monitor")
	    			.setIndicator("Monitors")
	    			.setContent(new Intent().setClass(this, DashboardActivity.class)));
	    mTabHost.addTab(mTabHost.newTabSpec("tab_graphs")
	    			.setIndicator("Graphs")
	    			.setContent(new Intent().setClass(this, DashboardActivity.class)));
	    mTabHost.addTab(mTabHost.newTabSpec("tab_activity")
	    			.setIndicator("Activity")
	    			.setContent(new Intent().setClass(this, DashboardActivity.class)));

	    mTabHost.setCurrentTab(0);
	}

}