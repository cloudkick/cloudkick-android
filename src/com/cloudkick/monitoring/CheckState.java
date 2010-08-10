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
		whence = state.getString("whence");
		serviceState = state.getString("service_state");
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
			stateColor = 0xFFF78181;
		}
		else if (serviceState.equals("NO-AGENT")) {
			status = "Agent Not Connected";
			stateColor = 0xFFBDBDBD;
		}
		else {
			Log.e("Check", "Unknown Service State: " + serviceState);
			status = state.getString("status");
			stateColor =  0xFFBDBDBD;
		}
	}
}