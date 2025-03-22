package com.windkracht8.rugbyrefereewatch;

import android.graphics.Color;
import android.view.Gravity;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.widget.TextViewCompat;

class Sinbin extends AppCompatTextView{
    final MatchData.sinbin sinbin;
    private final Main main;
    Sinbin(Main main, MatchData.sinbin sinbin, int color){
        super(main);
        this.main = main;
        this.sinbin = sinbin;

        main.addOnTouch(this);
        setHeight(Main.vh10);
        TextViewCompat.setAutoSizeTextTypeWithDefaults(this, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
        setTextColor(color);
        setIncludeFontPadding(false);
        setPadding(Main._10dp, 0, Main._10dp, 0);
        setGravity(Gravity.CENTER);
        update();
    }

    void update(){
        long remaining = sinbin.end - Main.timer_timer;
        if(Main.timer_status != Main.TimerStatus.RUNNING){
            remaining += System.currentTimeMillis() - Main.timer_start_time_off;
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
            main.beep();
        }
        String tmp = Main.prettyTimer(remaining);
        if(sinbin.who > 0){
            if(sinbin.team_is_home){
                tmp = "(" + sinbin.who + ") " + tmp;
            }else{
                tmp += " (" + sinbin.who + ")";
            }
        }
        setText(tmp);
    }
}
