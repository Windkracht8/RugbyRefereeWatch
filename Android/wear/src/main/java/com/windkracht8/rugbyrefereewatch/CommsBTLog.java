package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.content.pm.PackageInfo;
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

public class CommsBTLog extends ScrollView{
    private Main main;
    private LinearLayout commsBTLogItems;
    private static final ArrayList<String> log = new ArrayList<>();

    public CommsBTLog(Context context, AttributeSet attrs){
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(inflater == null){Toast.makeText(context, R.string.fail_show_log, Toast.LENGTH_SHORT).show(); return;}
        inflater.inflate(R.layout.comms_bt_log, this, true);
        commsBTLogItems = findViewById(R.id.commsBTLogItems);
        try{
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            String version = String.format("Version %s (%s)", packageInfo.versionName, packageInfo.getLongVersionCode());
            Log.d(Main.RRW_LOG_TAG, version);
            log.add(version);
        }catch(Exception e){
            Log.e(Main.RRW_LOG_TAG, "CommsBTLog getPackageInfo Exception: " + e.getMessage());
        }
    }

    void show(Main main){
        this.main = main;
        for(int i = commsBTLogItems.getChildCount(); i > 0; i--){
            commsBTLogItems.removeViewAt(i-1);
        }
        for(String line : log){
            addLine(line);
        }
        setVisibility(View.VISIBLE);
        fullScroll(View.FOCUS_UP);
        requestFocus();
    }
    void addToLog(String line){//Thread: Mostly called from background thread
        Log.d(Main.RRW_LOG_TAG, "CommsBTLog.addToLog: " + line);
        log.add(line);
        if(getVisibility() == View.VISIBLE){
            main.runOnUiThread(() -> addLine(line));
        }
    }
    private void addLine(String line){
        TextView tv = new TextView(getContext());
        tv.setText(line);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tv.setGravity(Gravity.CENTER);
        commsBTLogItems.addView(tv);
    }

}