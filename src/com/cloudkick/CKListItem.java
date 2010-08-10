package com.cloudkick;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

public abstract class CKListItem {
	public abstract View getItemView(Context context, View convertView, ViewGroup parent);
}
