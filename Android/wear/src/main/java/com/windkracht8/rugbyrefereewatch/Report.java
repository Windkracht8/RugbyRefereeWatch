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
        if(Main.isScreenRound) findViewById(R.id.llReport).setPadding(0, 0, 0, Main.vh25);
        for(int i = llReportItems.getChildCount(); i > 0; i--){
            llReportItems.removeViewAt(i-1);
        }
        addItem(main, llReportItems, match.home.tries + " tries " + match.away.tries);

        if(match.home.pen_tries > 0 || match.away.pen_tries > 0)
            addItem(main, llReportItems, match.home.pen_tries + " pen. tries " + match.away.pen_tries);
        if(match.points_con > 0 && (match.home.cons > 0 || match.away.cons > 0))
            addItem(main, llReportItems, match.home.cons + " conv. " + match.away.cons);
        if(match.points_goal > 0 && (match.home.goals > 0 || match.away.goals > 0))
            addItem(main, llReportItems, match.home.goals + " goals " + match.away.goals);
        addItem(main, llReportItems, match.home.tot + " score " + match.away.tot);
        addItem(main, llReportItems, "");

        if(match.home.yellow_cards > 0 || match.away.yellow_cards > 0)
            addItem(main, llReportItems, match.home.yellow_cards + " yellow cards " + match.away.yellow_cards);
        if(match.home.red_cards > 0 || match.away.red_cards > 0)
            addItem(main, llReportItems, match.home.red_cards + " red cards " + match.away.red_cards);
        if(Main.record_pens && (match.home.pens > 0 || match.away.pens > 0))
            addItem(main, llReportItems, match.home.pens + " penalties " + match.away.pens);
        addItem(main, llReportItems, "");

        for(MatchData.event event : match.events){
            if(event.what.equals("RESUME") || event.what.equals("TIME OFF")){
                continue;
            }
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
            addItem(main, llReportItems, text);
        }
        setVisibility(View.VISIBLE);
        fullScroll(View.FOCUS_UP);
        requestFocus();
    }
    private void addItem(Main main, LinearLayout llReportItems, String text){
        TextView item = new TextView(main, null, 0, R.style.textView_log);
        item.setText(text);
        llReportItems.addView(item);
        main.addOnTouch(item);
    }
}