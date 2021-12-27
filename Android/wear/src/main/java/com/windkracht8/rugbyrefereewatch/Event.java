package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;

public class Event extends TextView {
    public matchdata.event event_data;
    public Event(Context context) {
        super(context);
    }
    public Event(Context context, matchdata.event event_data) {
        super(context);
        this.event_data = event_data;
        String item = event_data.timer + ' ' + event_data.what;
        if(event_data.team != null){
            item += ' ' + event_data.team;
            if(event_data.who > 0){
                item += ' ' + event_data.who;
            }
        }
        this.setText(item);
        this.setGravity(Gravity.CENTER);
        this.setTextColor(Color.WHITE);
        this.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
    }
}