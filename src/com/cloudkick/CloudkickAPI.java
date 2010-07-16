/**
 * 
 */
package com.cloudkick;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;

import android.util.Log;


public class CloudkickAPI {
	private static final String TAG = "CloudkickAPI";
    private static String API_HOST = "api.cloudkick.com";
    private static String API_VERSION = "1.0";
    private CommonsHttpOAuthConsumer consumer;
    private HttpClient client;
    
	
	public CloudkickAPI(String key, String secret) {
		consumer = new CommonsHttpOAuthConsumer(key, secret);
		client = new DefaultHttpClient();
	}
	
	private String doRequest(String path) {
		StringBuilder body = new StringBuilder();
	    try {
			HttpGet request = new HttpGet("https://" + API_HOST + "/" + API_VERSION + path);
			consumer.sign(request);
		    HttpResponse response = client.execute(request);
		    InputStream is = response.getEntity().getContent();
			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
			String line;
			while ((line = rd.readLine()) != null) {
			    body.append(line);
			}
		} catch (OAuthMessageSignerException e) {
			//e.printStackTrace();
			return null;
		} catch (OAuthExpectationFailedException e) {
			//e.printStackTrace();
			return null;
		} catch (OAuthCommunicationException e) {
			//e.printStackTrace();
			return null;
		} catch (ClientProtocolException e) {
			//e.printStackTrace();
			return null;
		} catch (IOException e) {
			//e.printStackTrace();
			return null;
		}
		return body.toString();
	}

	public Node[] getNodes() {
		String body = doRequest("/query/nodes");
		Node[] nodes = null;
		try {
			JSONArray rawNodes = new JSONArray(body);
			nodes = new Node[rawNodes.length()];
			for (int i = 0; i < rawNodes.length(); i++) {
				nodes[i] = new Node(rawNodes.getJSONObject(i));
			}
		} catch (JSONException e) {
			return nodes;
			//e.printStackTrace();
		}
		Log.i(TAG, "Retrieved " + nodes.length + " Nodes");
		return nodes;
	}
}
