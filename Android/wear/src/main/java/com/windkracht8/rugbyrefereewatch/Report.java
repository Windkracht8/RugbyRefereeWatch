package com.windkracht8.rugbyrefereewatch;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Report extends Activity{
    private LinearLayout list;
    public static MatchData match;
    @Override public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        if(match == null){finishAndRemoveTask();}
        setContentView(R.layout.scroll_screen);
        ((TextView) findViewById(R.id.label)).setText(R.string.report_title);
        list = findViewById(R.id.list);
        if(Main.isScreenRound){
            list.setPadding(Main._10dp, Main._10dp, Main._10dp, Main.vh25);
        }
        findViewById(R.id.scrollView).requestFocus();

        addItem(match.home.tries + " tries " + match.away.tries);

        if(match.home.pen_tries > 0 || match.away.pen_tries > 0)
            addItem(match.home.pen_tries + " pen. tries " + match.away.pen_tries);
        if(match.points_con > 0 && (match.home.cons > 0 || match.away.cons > 0))
            addItem(match.home.cons + " conv. " + match.away.cons);
        if(match.points_goal > 0 && (match.home.goals > 0 || match.away.goals > 0))
            addItem(match.home.goals + " goals " + match.away.goals);
        addItem(match.home.tot + " score " + match.away.tot);
        addItem("");

        if(match.home.yellow_cards > 0 || match.away.yellow_cards > 0)
            addItem(match.home.yellow_cards + " yellow cards " + match.away.yellow_cards);
        if(match.home.red_cards > 0 || match.away.red_cards > 0)
            addItem(match.home.red_cards + " red cards " + match.away.red_cards);
        if(Main.record_pens && (match.home.pens > 0 || match.away.pens > 0))
            addItem(match.home.pens + " penalties " + match.away.pens);
        addItem("");

        for(MatchData.Event event : match.events){
            if(event.what.equals("RESUME") || event.what.equals("TIME OFF") || event.deleted){
                continue;
            }
            String text = Utils.prettyTimer(event.timer) + " ";
            switch(event.what){
                case "START":
                    text += getString(R.string.Start) + " " + Main.getPeriodName(this, event.period, match.period_count);
                    break;
                case "END":
                    text += getString(R.string.Result) + " " + Main.getPeriodName(this, event.period, match.period_count);
                    if(event.score != null){
                        text += " " + event.score;
                    }
                    break;
                default:
                    text += Translator.getEventTypeLocal(this, event.what);
                    break;
            }
            if(event.team != null){
                text += " " + Translator.getTeamLocal(this, event.team);
                if(event.who > 0){
                    text += ' ' + Integer.toString(event.who);
                }
            }
            addItem(text);
        }
    }
    @Override public void onDestroy(){
        super.onDestroy();
        match = null;
    }

    private void addItem(String text){
        TextView item = new TextView(this, null, 0, R.style.textView_report);
        item.setText(text);
        list.addView(item);
    }
}