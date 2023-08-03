package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class commsTypeAdapter extends BaseAdapter{
    final private Context context;
    final private int[] icons = {R.drawable.comms_type_tizen, R.drawable.comms_type_tizen, R.drawable.comms_type_wear};
    final private int[] names = {R.string.comms_type_bt, R.string.comms_type_tizen, R.string.comms_type_wear};
    private static int dp48 = 48;

    public commsTypeAdapter(Context context){
        this.context = context;
        this.context.setTheme(R.style.rrw);
        Resources r = context.getResources();
        dp48 = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, r.getDisplayMetrics()));
    }

    @Override
    public int getCount(){
        return icons.length;
    }

    @Override
    public Object getItem(int i){
        return null;
    }

    @Override
    public long getItemId(int i){
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup){
        TextView tv = new TextView(context);
        tv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp48));
        tv.setPadding(10,10,10,10);
        tv.setGravity(Gravity.FILL);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        tv.setCompoundDrawablePadding(24);
        tv.setCompoundDrawablesWithIntrinsicBounds(icons[i], 0, 0, 0);
        tv.setText(context.getString(names[i]));
        return tv;
    }
}
