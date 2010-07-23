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
					if (isFirst) isFirst = true;
					else tagBuilder.append(", " + tag);
				}
			}
			tagString = tagBuilder.toString();
		}
		return tagString;
	}
}
