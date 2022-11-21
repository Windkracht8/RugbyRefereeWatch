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
        if(inflater == null){Toast.makeText(context, R.string.fail_show_report, Toast.LENGTH_SHORT).show(); return;}
        inflater.inflate(R.layout.report, this, true);

        findViewById(R.id.llReport).setOnClickListener(v -> this.setVisibility(GONE));
    }

    public void load(MatchData match){
        LinearLayout list = findViewById(R.id.list);
        for(int i = list.getChildCount(); i > 0; i--){
            list.removeViewAt(i-1);
        }
        for(MatchData.event event : match.events){
            if(event.what.equals("RESUME") || event.what.equals("TIME OFF")){
                continue;
            }
            TextView tv = new TextView(getContext());
            String item = MainActivity.prettyTimer(event.timer) + " ";
            switch(event.what){
                case "START":
                    item += getContext().getString(R.string.Start) + " " + MainActivity.getPeriodName(getContext(), event.period, match.period_count);
                    break;
                case "END":
                    item += getContext().getString(R.string.Result) + " " + MainActivity.getPeriodName(getContext(), event.period, match.period_count);
                    if(event.score != null){
                        item += " " + event.score;
                    }
                    break;
                default:
                    item += translator.getEventTypeLocal(getContext(), event.what);
                    break;
            }
            if(event.team != null){
                item += " " + translator.getTeamLocal(getContext(), event.team);
                if(event.who > 0){
                    item += ' ' + Integer.toString(event.who);
                }
            }
            tv.setText(item);
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, MainActivity.vh7);
            list.addView(tv);
        }
    }
}