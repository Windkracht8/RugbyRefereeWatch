package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
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
    }

    void show(Main main){
        LinearLayout reportList = findViewById(R.id.reportList);
        for(int i = reportList.getChildCount(); i > 0; i--){
            reportList.removeViewAt(i-1);
        }

        for(MatchData.event event : Main.match.events){
            if(event.what.equals("RESUME") || event.what.equals("TIME OFF")){
                continue;
            }
            TextView tv = new TextView(main);
            String item = Main.prettyTimer(event.timer) + " ";
            switch(event.what){
                case "START":
                    item += main.getString(R.string.Start) + " " + Main.getPeriodName(main, event.period, Main.match.period_count);
                    break;
                case "END":
                    item += main.getString(R.string.Result) + " " + Main.getPeriodName(main, event.period, Main.match.period_count);
                    if(event.score != null){
                        item += " " + event.score;
                    }
                    break;
                default:
                    item += Translator.getEventTypeLocal(main, event.what);
                    break;
            }
            if(event.team != null){
                item += " " + Translator.getTeamLocal(main, event.team);
                if(event.who > 0){
                    item += ' ' + Integer.toString(event.who);
                }
            }
            tv.setText(item);
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);

            reportList.addView(tv);
            main.addOnTouch(tv);
        }
        setVisibility(View.VISIBLE);
        fullScroll(View.FOCUS_UP);
        requestFocus();
    }
}