package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Sinbin extends LinearLayout {
    public MatchData.sinbin sinbin;

    private final TextView timer;

    public Sinbin(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        inflater.inflate(R.layout.sinbin, this, true);

        timer = findViewById(R.id.timer);
        timer.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, MainActivity.vh10);
    }
    public void load(MatchData.sinbin sinbin, int col){
        this.sinbin = sinbin;
        this.setBackgroundColor(col);
        update();
    }
    public void update(){
        long remaining = sinbin.end - MainActivity.timer_timer;
        if(remaining < -((long)MainActivity.match.sinbin / 2 * 60000)){
            sinbin.hide = true;
        }
        if(sinbin.ended){
            return;
        }
        if(remaining <= 0){
            remaining = 0;
            sinbin.ended = true;
            timer.setTextColor(Color.RED);
            MainActivity.beep(getContext());
        }
        String tmp = MainActivity.prettyTimer(remaining);
        timer.setText(tmp);
    }
}
