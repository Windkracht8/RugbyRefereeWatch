package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class CommsLog extends ScrollView{
    public static final ArrayList<String> log = new ArrayList<>();

    public CommsLog(Context context, AttributeSet attrs){
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(inflater == null){Toast.makeText(context, R.string.fail_show_log, Toast.LENGTH_SHORT).show(); return;}
        inflater.inflate(R.layout.comms_log, this, true);
    }

    public void show(){
        LinearLayout commsLogItems = findViewById(R.id.commsLogItems);
        for(int i = commsLogItems.getChildCount(); i > 0; i--){
            commsLogItems.removeViewAt(i-1);
        }

        for(String line : log){
            TextView tv = new TextView(getContext());
            tv.setText(line);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            tv.setGravity(Gravity.CENTER);
            commsLogItems.addView(tv);
        }
        setVisibility(View.VISIBLE);
        fullScroll(View.FOCUS_UP);
        requestFocus();
    }
    public static void addToLog(String line){
        Log.d(Main.RRW_LOG_TAG, "CommsLog.add: " + line);
        log.add(line);
    }

}