package com.cloudkick;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class CKListLoadingSpinner extends CKListItem {
	@Override
	public View getItemView(Context context, View convertView, ViewGroup parent) {
		RelativeLayout loadingView;

		String inflater = Context.LAYOUT_INFLATER_SERVICE;
		LayoutInflater li = (LayoutInflater) context.getSystemService(inflater);

		if(convertView == null) {
			loadingView = new RelativeLayout(context);
			li.inflate(R.layout.loading_item, loadingView, true);

			Animation rotator = AnimationUtils.loadAnimation(context, R.anim.loading_rotate);
			((ImageView) loadingView.findViewById(R.id.loading_spinner)).startAnimation(rotator);
		}

		else {
			loadingView = (RelativeLayout) convertView;
		}

		return loadingView;
	}
}
