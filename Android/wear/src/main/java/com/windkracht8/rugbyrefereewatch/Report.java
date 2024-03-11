package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class Report extends ScrollView{
    public Report(Context context, AttributeSet attrs){
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        inflater.inflate(R.layout.report, this, true);
    }

    void show(Main main, MatchData match){
        LinearLayout llReportItems = findViewById(R.id.llReportItems);
        for(int i = llReportItems.getChildCount(); i > 0; i--){
            llReportItems.removeViewAt(i-1);
        }
        for(MatchData.event event : match.events){
            if(event.what.equals("RESUME") || event.what.equals("TIME OFF")){
                continue;
            }
            TextView item = new TextView(main, null, 0, R.style.textView_log);
            String text = Main.prettyTimer(event.timer) + " ";
            switch(event.what){
                case "START":
                    text += main.getString(R.string.Start) + " " + Main.getPeriodName(main, event.period, match.period_count);
                    break;
                case "END":
                    text += main.getString(R.string.Result) + " " + Main.getPeriodName(main, event.period, match.period_count);
                    if(event.score != null){
                        text += " " + event.score;
                    }
                    break;
                default:
                    text += Translator.getEventTypeLocal(main, event.what);
                    break;
            }
            if(event.team != null){
                text += " " + Translator.getTeamLocal(main, event.team);
                if(event.who > 0){
                    text += ' ' + Integer.toString(event.who);
                }
            }
            item.setText(text);

            llReportItems.addView(item);
            main.addOnTouch(item);
        }
        setVisibility(View.VISIBLE);
        fullScroll(View.FOCUS_UP);
        requestFocus();
    }
}