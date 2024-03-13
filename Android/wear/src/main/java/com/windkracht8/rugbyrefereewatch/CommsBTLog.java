package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;

public class CommsBTLog extends ScrollView{
    private Main main;
    private final LinearLayout llCommsBTLogItems;
    private static final ArrayList<String> log = new ArrayList<>();

    public CommsBTLog(Context context, AttributeSet attrs){
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        inflater.inflate(R.layout.comms_bt_log, this, true);
        llCommsBTLogItems = findViewById(R.id.llCommsBTLogItems);
        try{
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            String version = String.format("Version %s (%s)", packageInfo.versionName, packageInfo.getLongVersionCode());
            addToLog(version);
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "CommsBTLog getPackageInfo Exception: " + e.getMessage());
        }
    }

    void show(Main main){
        this.main = main;
        for(int i = llCommsBTLogItems.getChildCount(); i > 0; i--){
            llCommsBTLogItems.removeViewAt(i-1);
        }
        for(String line : log){
            addTextView(line);
        }
        setVisibility(View.VISIBLE);
        fullScroll(View.FOCUS_UP);
        requestFocus();
    }
    void addToLog(String line){//Thread: Mostly called from background thread
        Log.d(Main.LOG_TAG, "CommsBTLog.addToLog: " + line);
        log.add(line);
        if(getVisibility() == View.VISIBLE){
            main.runOnUiThread(() -> addTextView(line));
        }
    }
    private void addTextView(String line){
        TextView tv = new TextView(getContext(), null, 0, R.style.textView_log);
        tv.setText(line);
        llCommsBTLogItems.addView(tv);
    }
}