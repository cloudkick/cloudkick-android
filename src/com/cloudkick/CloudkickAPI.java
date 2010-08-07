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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.ArrayList;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.cloudkick.exceptions.BadCredentialsException;
import com.cloudkick.exceptions.EmptyCredentialsException;
import com.cloudkick.monitoring.Check;


public class CloudkickAPI {
	private static final String TAG = "CloudkickAPI";
	private static String API_HOST = "api.cloudkick.com";
	private static String API_VERSION = "1.0";
	private final String key;
	private final String secret;
	private final HttpClient client;
	private SharedPreferences prefs = null;

	public CloudkickAPI(Context context) throws EmptyCredentialsException {
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
		key = prefs.getString("editKey", "");
		secret = prefs.getString("editSecret", "");
		if (key == "" || secret == "") {
				throw new EmptyCredentialsException();
		}

		HttpParams params = new BasicHttpParams();
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(params, HTTP.DEFAULT_CONTENT_CHARSET);
		HttpProtocolParams.setUseExpectContinue(params, true);

		SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		registry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));

		ClientConnectionManager connman = new ThreadSafeClientConnManager(params, registry);
		client = new DefaultHttpClient(connman, params);
	}

	private String doRequest(String path) throws BadCredentialsException {
		StringBuilder body = new StringBuilder();
		try {
			HttpGet request = new HttpGet("https://" + API_HOST + "/" + API_VERSION + path);
			OAuthConsumer consumer = new CommonsHttpOAuthConsumer(key, secret);
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
		// All node names are quoted for safety
		String encodedNode = URLEncoder.encode("\"" + nodeName + "\"");
		String body = doRequest("/query/nodes?query=node:" + encodedNode);
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

	public Check getCheck(String nodeId, String checkName) throws BadCredentialsException {
		String body = doRequest("/query/node/" + nodeId + "/check/" + checkName);
		try {
			return new Check(new JSONObject(body));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (NullPointerException e) {
			// TODO: Figure out what causes these
			return null;
		}
	}
}
