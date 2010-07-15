package com.cloudkick;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Node {
	String name;
	String providerID;
	String[] tags;
	Integer color;
	String providerName;
	String ipAddress;
	String id;
	String agentState;
	
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
			agentState = obj.getString("agentState");
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String getName() {
		return name;
	}


	public String getProviderID() {
		return providerID;
	}


	public String[] getTags() {
		return tags;
	}


	public Integer getColor() {
		return color;
	}


	public String getProviderName() {
		return providerName;
	}


	public String getIpAddress() {
		return ipAddress;
	}


	public String getId() {
		return id;
	}


	public String getAgentState() {
		return agentState;
	}
}
