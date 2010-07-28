/**
 *
 */
package com.cloudkick;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.cloudkick.exceptions.BadCredentialsException;
import com.cloudkick.exceptions.EmptyCredentialsException;


public class CloudkickAPI {
	private static final String TAG = "CloudkickAPI";
    private static String API_HOST = "api.cloudkick.com";
    private static String API_VERSION = "1.0";
    private final CommonsHttpOAuthConsumer consumer;
    private final HttpClient client;
    private SharedPreferences prefs = null;

	public CloudkickAPI(Context context) throws EmptyCredentialsException {
	    prefs = PreferenceManager.getDefaultSharedPreferences(context);
	    String key = prefs.getString("editKey", "");
	    String secret = prefs.getString("editSecret", "");
	    if (key == "" || secret == "") {
	    		throw new EmptyCredentialsException();
	    }
		consumer = new CommonsHttpOAuthConsumer(key, secret);
		client = new DefaultHttpClient();
	}

	private String doRequest(String path) throws BadCredentialsException {
		StringBuilder body = new StringBuilder();
	    try {
			HttpGet request = new HttpGet("https://" + API_HOST + "/" + API_VERSION + path);
			consumer.sign(request);
		    HttpResponse response = client.execute(request);
		    if (response.getStatusLine().getStatusCode() == 401) {
		    	throw new BadCredentialsException();
		    }
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

	public ArrayList<Node> getNodes() throws BadCredentialsException {
		String body = doRequest("/query/nodes");
		ArrayList<Node> nodes = new ArrayList<Node>();
		try {
			JSONArray rawNodes = new JSONArray(body);
			int rawCount = rawNodes.length();
			for (int i = 0; i < rawCount; i++) {
				nodes.add(new Node(rawNodes.getJSONObject(i)));
			}
		} catch (JSONException e) {
			return nodes;
			//e.printStackTrace();
		}
		Log.i(TAG, "Retrieved " + nodes.size() + " Nodes");
		return nodes;
	}

	public Node getNode(String nodeName) throws BadCredentialsException {
		String body = doRequest("/query/nodes?query=node:" + nodeName);
		try {
			Node node = new Node(new JSONArray(body).getJSONObject(0));
			Log.i(TAG, "Retrieved node: " + node.name);
			return node;
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
}
