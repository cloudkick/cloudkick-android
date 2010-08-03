package com.cloudkick;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class NodesAdapter extends ArrayAdapter<Node> {
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

		// Set the a color representing the state
		TextView statusView = (TextView)nodeView.findViewById(R.id.node_item_status);
		statusView.setBackgroundDrawable(new ColorDrawable(node.getStateColor()));

		// Set the background
		ColorDrawable transparent = new ColorDrawable(Color.TRANSPARENT);
		ColorDrawable opaque = new ColorDrawable(node.color);
		StateListDrawable bg = new StateListDrawable();
		bg.addState(new int[] {android.R.attr.state_selected}, transparent);
		bg.addState(new int[] {android.R.attr.state_pressed}, transparent);
		bg.addState(new int[] {}, opaque);
		nodeView.setBackgroundDrawable(bg);

		// Set the name and tags
		TextView nameText = (TextView)nodeView.findViewById(R.id.name);
		TextView tagsText = (TextView)nodeView.findViewById(R.id.tags);
		nameText.setText(node.name);
		tagsText.setText(node.getTagString());

		return nodeView;
	}
}