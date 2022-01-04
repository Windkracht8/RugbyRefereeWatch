package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class Report extends ScrollView {
    public Report(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        inflater.inflate(R.layout.report, this, true);

        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
        params.setMargins(0, MainActivity.vh10, 0, MainActivity.vh10);
        findViewById(R.id.list).setLayoutParams(params);
        findViewById(R.id.list).setMinimumHeight(MainActivity.vh80);
    }

    public void load(matchdata match){
        LinearLayout list = findViewById(R.id.list);
        for(int i = list.getChildCount(); i > 0; i--){
            list.removeViewAt(i-1);
        }
        for(matchdata.event event : match.events){
            if(event.what.equals("Resume time") || event.what.equals("Time off")){
                continue;
            }
            TextView tv = new TextView(getContext());
            String item = event.timer + ' ' + event.what;
            if(event.team != null){
                item += ' ' + event.team;
                if(event.who > 0){
                    item += ' ' + Integer.toString(event.who);
                }
            }
            tv.setText(item);
            tv.setGravity(Gravity.CENTER);
            tv.setTextColor(Color.WHITE);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            list.addView(tv);
        }
    }
}