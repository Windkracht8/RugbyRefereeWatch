package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class osAdapter extends BaseAdapter {
    final private Context context;
    final private int[] icons = {R.drawable.os_tizen, R.drawable.os_wear};
    final private int[] names = {R.string.os_tizen, R.string.os_wear};

    public osAdapter(Context context) {
        this.context = context;
        this.context.setTheme(R.style.AppTheme);
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
