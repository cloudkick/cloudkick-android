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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class CKListLoadingSpinner extends CKListItem {
	private static final long serialVersionUID = -7779364040534960512L;

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

	public int compareTo(CKListItem another) {
		return -1;
	}
}
