package com.cloudkick;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class NodesAdapter extends ArrayAdapter<Node> {
	private int resource;

	public NodesAdapter(Context context, int resource, Node[] nodes)
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
		
		// Set the background
		nodeView.setBackgroundDrawable(new ColorDrawable(node.getColor()));
		
		// Set the name and tags
		TextView nameText = (TextView)nodeView.findViewById(R.id.name);
		TextView tagsText = (TextView)nodeView.findViewById(R.id.tags);
		nameText.setText(node.name);
		tagsText.setText(tagString.toString());

		return nodeView;
	}
}