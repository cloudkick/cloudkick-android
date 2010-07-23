package com.cloudkick;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class NodesAdapter extends ArrayAdapter<Node> {
	private static String TAG = "NodesAdapter";
	private final int resource;

	public NodesAdapter(Context context, int resource, ArrayList<Node> nodes)
	{
		super(context, resource, nodes);
		this.resource = resource;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		RelativeLayout nodeView;
		Node node = getItem(position);

		String inflater = Context.LAYOUT_INFLATER_SERVICE;
		LayoutInflater li = (LayoutInflater)getContext().getSystemService(inflater);

		if(convertView==null) {
			nodeView = new RelativeLayout(getContext());
			li.inflate(resource, nodeView, true);
		}

		else {
			nodeView = (RelativeLayout) convertView;
		}

		// Build the tag string
		StringBuilder tagString = new StringBuilder();
		if (node.getTags().length > 0) {
			tagString.append(node.getTags()[0]);
			for (int i = 1; i < node.tags.length; i++) {
				tagString.append(", ");
				tagString.append(node.getTags()[i]);
			}
		}

		// Set the state in this abomination
		int color = 0xFFFF9C52;
		if (node.status == "pending") color = 0xFFFFD652;
		else if (node.status.equals("running")) color = 0xFF75BA14;
		else if (node.status.equals("rebooting")) color = 0xFF6dafb5;
		else if (node.status.equals("rebuilding")) color = 0xFF234c59;
		else if (node.status.equals("terminated")) color = 0xFFd95338;
		else if (node.status.equals("terminating")) color = 0xFFd9533;
		Log.i(TAG, "Status: " + node.status);
		TextView statusView = (TextView)nodeView.findViewById(R.id.node_item_status);
		statusView.setBackgroundDrawable(new ColorDrawable(color));

		// Set the background
		ColorDrawable transparent = new ColorDrawable(Color.TRANSPARENT);
		ColorDrawable opaque = new ColorDrawable(node.getColor());
		StateListDrawable bg = new StateListDrawable();
		bg.addState(new int[] {android.R.attr.state_selected}, transparent);
		bg.addState(new int[] {android.R.attr.state_pressed}, transparent);
		bg.addState(new int[] {}, opaque);
		nodeView.setBackgroundDrawable(bg);

		// Set the name and tags
		TextView nameText = (TextView)nodeView.findViewById(R.id.name);
		TextView tagsText = (TextView)nodeView.findViewById(R.id.tags);
		nameText.setText(node.name);
		tagsText.setText(tagString.toString());

		return nodeView;
	}
}