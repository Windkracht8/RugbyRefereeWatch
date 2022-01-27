package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class Report extends ScrollView{
    public Report(Context context, AttributeSet attrs){
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(inflater == null){Toast.makeText(context, "Failed to show report", Toast.LENGTH_SHORT).show(); return;}
        inflater.inflate(R.layout.report, this, true);

        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
        params.setMargins(0, MainActivity.vh5, 0, MainActivity.vh5);
        findViewById(R.id.list_wrap).setLayoutParams(params);
        findViewById(R.id.list_wrap).setMinimumHeight(MainActivity.vh90);
    }

    public void load(MatchData match){
        LinearLayout list = findViewById(R.id.list);
        for(int i = list.getChildCount(); i > 0; i--){
            list.removeViewAt(i-1);
        }
        for(MatchData.event event : match.events){
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
            tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            list.addView(tv);
        }
    }
}