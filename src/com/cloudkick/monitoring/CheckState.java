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

package com.cloudkick.monitoring;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.json.JSONException;
import org.json.JSONObject;

import android.text.format.Time;
import android.util.Log;

public class CheckState implements Serializable {
	private static final long serialVersionUID = -2297216122693619863L;
	private SimpleDateFormat whenceWireFormat;
	public final String status;
	public String whence;
	public final String serviceState;
	public final Integer stateColor;
	public final String stateSymbol;
	public final Integer priority;

	public CheckState(JSONObject state) throws JSONException {
		// Grab the "whence" if available
		if (state.has("whence")) {
			whenceWireFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			whenceWireFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
			String whenceString = state.getString("whence");
			try {
				long whenceMillis = whenceWireFormat.parse(whenceString).getTime();
				Time now = new Time();
				now.setToNow();
				long diffMillis = ((now.toMillis(true)) - whenceMillis);
				if (diffMillis < 3600*1000) {
					whence = String.format("%d m", diffMillis/(1000*60));
				}
				else if (diffMillis < (24*3600*1000)) {
					long mins = (diffMillis / (1000 * 60)) % 60;
					long hours = (diffMillis / (1000 * 3600));
					whence = String.format("%d h, %d m", hours, mins);
				}
				else if (diffMillis < (7*24*3600*1000)){
					long hours = (diffMillis / (1000 * 60 * 60)) % 24;
					long days = (diffMillis / (1000 * 60 * 60 * 24));
					whence = String.format("%d d, %d h", days, hours);
				}
				else {
					long days = (diffMillis / (1000 * 60 * 60 * 24)) % 7;
					long weeks = (diffMillis / (1000 * 60 * 60 * 24 * 7));
					whence = String.format("%d w, %d d", weeks, days);
				}
			} catch (ParseException e) {
				whence = null;
			}
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
			stateColor = 0xFF088A08;
			stateSymbol = "\u2714";
			priority = 4;
		}
		else if (serviceState.equals("ERROR")) {
			status = state.getString("status");
			stateColor = 0xFFE34648;
			stateSymbol = "\u2718";
			priority = 0;
		}
		else if (serviceState.equals("WARNING")) {
			status = state.getString("status");
			stateColor = 0xFFDF7401;
			stateSymbol = "!";
			priority = 1;
		}
		else if (serviceState.equals("NO-AGENT")) {
			status = "Agent Not Connected";
			stateColor = 0xFF6E6E6E;
			stateSymbol = "?";
			priority = 2;
		}
		else if (serviceState.equals("UNKNOWN")) {
			status = "No Data Available";
			stateColor = 0xFF6E6E6E;
			stateSymbol = "?";
			priority = 3;
		}
		else {
			Log.e("Check", "Unknown Service State: " + serviceState);
			status = state.getString("status");
			stateColor =  0xFF6E6E6E;
			stateSymbol = "?";
			priority = 3;
		}
	}
}