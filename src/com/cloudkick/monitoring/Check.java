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

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.cloudkick.CKListItem;
import com.cloudkick.R;

public class Check extends CKListItem {
	private static final long serialVersionUID = 5457450782300306447L;
	public final CheckState previousState;
	public final CheckState latestState;
	public final String type;
	public final String label;
	public final String id;

	public Check(JSONObject rawCheck) throws JSONException {
		previousState = new CheckState(rawCheck.getJSONObject("previous_state"));
		latestState = new CheckState(rawCheck.getJSONObject("latest_state"));
		type = rawCheck.getString("type");
		id = rawCheck.getString("id");
		if (type.equals("PLUGIN")) {
			label = rawCheck.getJSONObject("details").getString("check");
		}
		else {
			label = type;
		}
	}

	@Override
	public View getItemView(Context context, View convertView, ViewGroup parent) {
		RelativeLayout checkView;

		String inflater = Context.LAYOUT_INFLATER_SERVICE;
		LayoutInflater li = (LayoutInflater) context.getSystemService(inflater);

		if(convertView == null) {
			checkView = new RelativeLayout(context);
			li.inflate(R.layout.node_detail, checkView, true);
		}

		else {
			checkView = (RelativeLayout) convertView;
		}

		// Set the background
		ColorDrawable transparent = new ColorDrawable(Color.TRANSPARENT);
		ColorDrawable opaque = new ColorDrawable(latestState.stateColor);
		StateListDrawable bg = new StateListDrawable();
		bg.addState(new int[] {android.R.attr.state_selected}, transparent);
		bg.addState(new int[] {android.R.attr.state_pressed}, transparent);
		bg.addState(new int[] {}, opaque);
		checkView.setBackgroundDrawable(bg);

		((TextView) checkView.findViewById(R.id.detail_label)).setText(label);
		((TextView) checkView.findViewById(R.id.detail_value)).setText(latestState.status);

		return checkView;
	}
}