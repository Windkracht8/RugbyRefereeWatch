package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.TextView;

public class Sinbin extends TextView{
    public MatchData.sinbin sinbin;
    public Sinbin(Context context, AttributeSet attrs){super(context, attrs);}
    public Sinbin(Main main, MatchData.sinbin sinbin, String color){
        super(main);
        this.sinbin = sinbin;

        main.addOnTouch(this);

        setHeight(Main.vh10);
        setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM);
        setTextColor(main.getColorFG(color));
        setIncludeFontPadding(false);
        int ll_in_sc_padding = getResources().getDimensionPixelSize(R.dimen.ll_in_sc_padding);
        setPadding(ll_in_sc_padding, 0, ll_in_sc_padding, 0);
        setGravity(Gravity.CENTER);
        update();
    }

    public void update(){
        long remaining = sinbin.end - Main.timer_timer;
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
