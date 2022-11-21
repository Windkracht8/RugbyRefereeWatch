package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;

public class Event extends androidx.appcompat.widget.AppCompatTextView{
    public MatchData.event event;
    public Event(Context context){
        super(context);
    }
    public Event(Context context, MatchData.event event){
        super(context);
        this.event = event;
        String item = MainActivity.prettyTimer(event.timer) + " " + translator.getEventTypeLocal(context, event.what);
        if(event.team != null){
            item += " " + translator.getTeamLocal(context, event.team);
            if(event.who > 0){
                item = item + " " + event.who;
            }
        }
        this.setText(item);
        this.setGravity(Gravity.CENTER);
        this.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
    }
}
