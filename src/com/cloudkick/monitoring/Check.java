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