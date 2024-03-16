package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ExtraTime extends LinearLayout{
    public ExtraTime(Context context, AttributeSet attrs){
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        inflater.inflate(R.layout.extra_time, this, true);
    }
    void onCreateMain(Main main){
        TextView extra_time_up = findViewById(R.id.extra_time_up);
        TextView extra_time_2min = findViewById(R.id.extra_time_2min);
        TextView extra_time_5min = findViewById(R.id.extra_time_5min);
        TextView extra_time_10min = findViewById(R.id.extra_time_10min);
        extra_time_up.setOnClickListener(v -> main.extraTimeChange(0));
        extra_time_2min.setOnClickListener(v -> main.extraTimeChange(2));
        extra_time_5min.setOnClickListener(v -> main.extraTimeChange(5));
        extra_time_10min.setOnClickListener(v -> main.extraTimeChange(10));
        if(getResources().getConfiguration().fontScale > 1.1){
            extra_time_up.setIncludeFontPadding(false);
            extra_time_2min.setIncludeFontPadding(false);
            extra_time_5min.setIncludeFontPadding(false);
            extra_time_10min.setIncludeFontPadding(false);
        }
    }

}
