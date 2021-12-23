package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class osAdapter extends BaseAdapter {
    Context context;
    int[] icons;
    int[] names;

    public osAdapter(Context context, int[] icons, int[] names) {
        this.context = context;
        this.context.setTheme(R.style.AppTheme);
        this.icons = icons;
        this.names = names;
    }

    @Override
    public int getCount() {
        return icons.length;
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        TextView tv = new TextView(context);
        tv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        tv.setPadding(0,10,0,10);
        tv.setTextSize(20);
        tv.setCompoundDrawablesWithIntrinsicBounds(icons[i], 0, 0, 0);
        tv.setText(context.getString(names[i]));
        return tv;
    }
}
