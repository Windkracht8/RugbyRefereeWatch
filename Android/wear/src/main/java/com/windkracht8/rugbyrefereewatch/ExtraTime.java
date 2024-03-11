package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

public class ExtraTime extends LinearLayout{
    public ExtraTime(Context context, AttributeSet attrs){
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        inflater.inflate(R.layout.extra_time, this, true);
    }
    void onCreateMain(Main main){
        findViewById(R.id.extra_time_up).setOnClickListener(v -> main.extraTimeChange(0));
        findViewById(R.id.extra_time_2min).setOnClickListener(v -> main.extraTimeChange(2));
        findViewById(R.id.extra_time_5min).setOnClickListener(v -> main.extraTimeChange(5));
        findViewById(R.id.extra_time_10min).setOnClickListener(v -> main.extraTimeChange(10));
    }

}
