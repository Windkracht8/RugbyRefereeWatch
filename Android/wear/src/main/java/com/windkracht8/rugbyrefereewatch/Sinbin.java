package com.windkracht8.rugbyrefereewatch;

import android.graphics.Color;
import android.view.Gravity;
import android.widget.TextView;

class Sinbin extends TextView{
    final MatchData.sinbin sinbin;
    Sinbin(Main main, MatchData.sinbin sinbin, String color){
        super(main);
        this.sinbin = sinbin;

        main.addOnTouch(this);

        setHeight(Main.vh10);
        setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM);
        setTextColor(main.getColorFG(color));
        setIncludeFontPadding(false);
        setPadding(Main.dp10, 0, Main.dp10, 0);
        setGravity(Gravity.CENTER);
        update();
    }

    void update(){
        long remaining = sinbin.end - Main.timer_timer;
        if(Main.timer_status != Main.TimerStatus.RUNNING){
            remaining += Main.getCurrentTimestamp() - Main.timer_start_time_off;
        }
        if(remaining < -60000){
            sinbin.hide = true;
        }
        if(sinbin.ended){
            return;
        }
        if(remaining <= 0){
            remaining = 0;
            sinbin.ended = true;
            setTextColor(Color.RED);
            Main.beep();
        }
        String tmp = Main.prettyTimer(remaining);
        setText(tmp);
    }
}
