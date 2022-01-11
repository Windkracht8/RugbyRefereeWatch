package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

public class Correct extends ScrollView {
    private MatchData match;
    public Correct(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        inflater.inflate(R.layout.correct, this, true);

        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
        params.setMargins(0, MainActivity.vh10, 0, MainActivity.vh10);
        findViewById(R.id.list_wrap).setLayoutParams(params);
        findViewById(R.id.list_wrap).setMinimumHeight(MainActivity.vh80);
    }
    public void load(MatchData match){
        this.match = match;
        LinearLayout list = findViewById(R.id.list);
        for(int i = list.getChildCount(); i > 0; i--){
            list.removeViewAt(i-1);
        }
        for(int i = match.events.size(); i > 0; i--){
            MatchData.event event_data = match.events.get(i-1);
            if(!event_data.what.equals("TRY") &&
                    !event_data.what.equals("CONVERSION") &&
                    !event_data.what.equals("GOAL") &&
                    !event_data.what.equals("YELLOW CARD") &&
                    !event_data.what.equals("RED CARD")
            ){
                continue;
            }
            Event event_ui = new Event(getContext(), event_data);
            list.addView(event_ui);
            event_ui.setOnClickListener(this::eventClicked);
        }
    }
    public void eventClicked(View v){
        Event event_ui = (Event) v;
        match.removeEvent(event_ui.event_data);
        this.setVisibility(View.GONE);
        this.performClick();
    }
}
