package com.cloudkick.monitoring;

import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;



public class Check {
	public String status;
	// This doesn't support string metrics
	// Should do that with some sort of generic type factory?
	public HashMap<String, Float> metrics = new HashMap<String, Float>();

	public Check(JSONObject rawCheck) {
		try {
			status = rawCheck.getString("status");
			JSONArray rawMetrics = rawCheck.getJSONArray("metrics");
			final int metricCount = rawMetrics.length();
			for (int i = 0; i < metricCount; i++) {
				final JSONObject rawMetric = rawMetrics.getJSONObject(i);
				metrics.put(rawMetric.getString("name"), new Float(rawMetric.getString("value")));
				Log.i("Check", rawMetric.getString("name") + ": " + rawMetric.getString("value"));
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OutOfMemoryError e) {
			// This happens on certain Float values when using < Android 2.2
			e.printStackTrace();
		}
	}
}