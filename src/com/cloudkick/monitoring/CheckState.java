package com.cloudkick.monitoring;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class CheckState {
	public final String status;
	public final String whence;
	public final String serviceState;
	public final Integer stateColor;

	public CheckState(JSONObject state) throws JSONException {
		// Grab the "whence" if available
		if (state.has("whence")) {
			whence = state.getString("whence");
		}
		else {
			whence = null;
		}

		// Grab the "service_state" if available
		if (state.has("service_state")) {
			serviceState = state.getString("service_state");
		}
		else {
			serviceState = "UNKNOWN";
		}

		// Depending on the serviceState set the status and color
		if (serviceState.equals("OK")) {
			status = state.getString("status");
			stateColor = 0xFFA9F5A9;
		}
		else if (serviceState.equals("WARNING")) {
			status = state.getString("status");
			stateColor = 0xFFFAAC58;
		}
		else if (serviceState.equals("ERROR")) {
			status = state.getString("status");
			stateColor = 0xFFE34648;
		}
		else if (serviceState.equals("NO-AGENT")) {
			status = "Agent Not Connected";
			stateColor = 0xFFBDBDBD;
		}
		else if (serviceState.equals("UNKNOWN")) {
			status = "No Data Available";
			stateColor = 0xFFBDBDBD;
		}
		else {
			Log.e("Check", "Unknown Service State: " + serviceState);
			status = state.getString("status");
			stateColor =  0xFFBDBDBD;
		}
	}
}