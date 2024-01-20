package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class CommsDebugLog extends ScrollView{
    public CommsDebugLog(Context context, AttributeSet attrs){
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(inflater == null){Toast.makeText(context, R.string.fail_show_report, Toast.LENGTH_SHORT).show(); return;}
        inflater.inflate(R.layout.comms_debug_log, this, true);
    }

    public void show(Main main){
        LinearLayout commsDebugLogItems = findViewById(R.id.commsDebugLogItems);
        for(int i = commsDebugLogItems.getChildCount(); i > 0; i--){
            commsDebugLogItems.removeViewAt(i-1);
        }

        for(String line : Comms.comms_debug_log){
            Log.d(Main.RRW_LOG_TAG, "CommsDebugLog: " + line);
            TextView tv = new TextView(getContext());
            tv.setText(line);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            main.addOnTouch(tv);
            commsDebugLogItems.addView(tv);
        }
        setVisibility(View.VISIBLE);
        fullScroll(View.FOCUS_UP);
        requestFocus();
    }
}