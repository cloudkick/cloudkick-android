package com.cloudkick;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class NodeViewActivity extends Activity {
	private static final String TAG = "Node";
	private Node node;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle data = this.getIntent().getExtras();
		node = (Node) data.getSerializable("node");

		RelativeLayout nodeView;
		String inflater = Context.LAYOUT_INFLATER_SERVICE;
		LayoutInflater li = (LayoutInflater) getSystemService(inflater);

		nodeView = new RelativeLayout(this);
		li.inflate(R.layout.node_view, nodeView, true);

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
		TextView nameText = (TextView) nodeView
				.findViewById(R.id.node_detail_name);
		TextView tagsText = (TextView) nodeView
				.findViewById(R.id.node_detail_tags);
		nameText.setText(node.getName());
		tagsText.setText(tagString.toString());

		setContentView(nodeView);
		setTitle("Node: " + node.getName());
	}
}