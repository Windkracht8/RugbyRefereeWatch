package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;

public class Sinbin extends TextView{
    public MatchData.sinbin sinbin;
    public Sinbin(Context context, AttributeSet attrs){super(context, attrs);}
    public Sinbin(Main main, MatchData.sinbin sinbin, boolean isHome, String color){
        super(main);
        this.sinbin = sinbin;

        main.addOnTouch(this);

        setTextSize(TypedValue.COMPLEX_UNIT_PX, Main.vh10);
        setBackgroundColor(main.getColorBG(color));
        setTextColor(main.getColorFG(color));
        setIncludeFontPadding(false);
        int ll_in_sc_padding = getResources().getDimensionPixelSize(R.dimen.ll_in_sc_padding);
        setPadding(ll_in_sc_padding, 0, ll_in_sc_padding, 0);
        if(isHome){
            setGravity(Gravity.LEFT);
        }else{
            setGravity(Gravity.RIGHT);
        }
        update();
    }

    public void update(){
        long remaining = sinbin.end - Main.timer_timer;
        if(remaining < -((long) Main.match.sinbin / 2 * 60000)){
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
