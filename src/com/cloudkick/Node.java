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
package com.cloudkick;

import java.io.Serializable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Node implements Serializable {
	private static final long serialVersionUID = -6829386412299389386L;
	public String name;
	public String providerID;
	public String[] tags;
	public Integer color;
	public String providerName;
	public String ipAddress;
	public String id;
	public String agentState;
	public String status;
	private String tagString = null;
	private Integer stateColor = null;

	public Node(JSONObject obj) {
		try {
			name = obj.getString("name");
			providerID = obj.getString("provider_id");
			JSONArray tagArray = obj.getJSONArray("tags");
			tags = new String[tagArray.length()];
			for (int i = 0; i < tagArray.length(); i++) {
				tags[i] = tagArray.getString(i);
			}
			color = Integer.parseInt(obj.getString("color").replace("#", ""), 16) | (0xFF << 24);
			providerName = obj.getString("provider_name");
			ipAddress = obj.getString("ipaddress");
			id = obj.getString("id");
			agentState = obj.getString("agent_state");
			status = obj.getString("status");

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String getTagString() {
		if (tagString == null) {
			// Build the tag string
			StringBuilder tagBuilder = new StringBuilder();
			if (tags.length > 0) {
				tagBuilder.append(tags[0]);
				boolean isFirst = true;
				for (String tag: tags) {
					if (isFirst) isFirst = false;
					else tagBuilder.append(", " + tag);
				}
			}
			tagString = tagBuilder.toString();
		}
		return tagString;
	}

	public Integer getStateColor() {
		if (stateColor == null) {
			stateColor = 0xFFFF9C52;
			if (status == "pending") stateColor = 0xFFFFD652;
			else if (status.equals("running")) stateColor = 0xFF75BA14;
			else if (status.equals("rebooting")) stateColor = 0xFF6dafb5;
			else if (status.equals("rebuilding")) stateColor = 0xFF234c59;
			else if (status.equals("terminated")) stateColor = 0xFFd95338;
			else if (status.equals("terminating")) stateColor = 0xFFd9533;
		}
		return stateColor;
	}
}
