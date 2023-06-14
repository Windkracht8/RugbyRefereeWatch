package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

public class Correct extends ScrollView{
    private MatchData match;
    public Correct(Context context, AttributeSet attrs){
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(inflater == null){Toast.makeText(context, R.string.fail_show_correct, Toast.LENGTH_SHORT).show(); return;}
        inflater.inflate(R.layout.correct, this, true);
    }
    public void show(MainActivity ma, MatchData match){
        this.match = match;
        LinearLayout correctList = findViewById(R.id.correctList);
        for(int i = correctList.getChildCount(); i > 0; i--){
            correctList.removeViewAt(i-1);
        }
        for(int i = match.events.size(); i > 0; i--){
            MatchData.event event_data = match.events.get(i-1);
            if(!event_data.what.equals("TRY") &&
                    !event_data.what.equals("CONVERSION") &&
                    !event_data.what.equals("PENALTY TRY") &&
                    !event_data.what.equals("PENALTY") &&
                    !event_data.what.equals("GOAL") &&
                    !event_data.what.equals("YELLOW CARD") &&
                    !event_data.what.equals("RED CARD")
            ){
                continue;
            }
            Event event_ui = new Event(getContext(), event_data);
            correctList.addView(event_ui);
            event_ui.setOnClickListener(this::eventClicked);
            ma.addOnTouch(event_ui);
        }
        this.setVisibility(View.VISIBLE);
        this.fullScroll(View.FOCUS_UP);
        this.animate().x(0).scaleX(1f).scaleY(1f).setDuration(0).start();
    }
    public void eventClicked(View v){
        if(MainActivity.draggingEnded+100 > MainActivity.getCurrentTimestamp()) return;
        Event event_ui = (Event) v;
        match.removeEvent(event_ui.event);
        this.setVisibility(View.GONE);
        this.performClick();
    }
}
