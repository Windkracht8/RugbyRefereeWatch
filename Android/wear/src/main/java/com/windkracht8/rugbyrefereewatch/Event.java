package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;

public class Event extends TextView{
    public MatchData.event event;
    public Event(Context context){
        super(context);
    }
    public Event(Context context, MatchData.event event){
        super(context);
        this.event = event;
        String item = Main.prettyTimer(event.timer) + " " + Translator.getEventTypeLocal(context, event.what);
        if(event.team != null){
            item += " " + Translator.getTeamLocal(context, event.team);
            if(event.who > 0){
                item = item + " " + event.who;
            }
        }
        setText(item);
        setGravity(Gravity.CENTER);
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        setBackgroundResource(R.drawable.conf_item_bg);
        setMinHeight(getResources().getDimensionPixelSize(R.dimen.minTouchSize));
    }
}
