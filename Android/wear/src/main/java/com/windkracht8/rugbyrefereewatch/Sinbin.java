package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class Sinbin extends LinearLayout{
    public MatchData.sinbin sinbin;
    private TextView timer;
    public Sinbin(Context context, AttributeSet attrs){super(context, attrs);}
    public Sinbin(Context context, AttributeSet attrs, Main main, MatchData.sinbin sinbin, boolean isHome, int col){
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(inflater == null){Toast.makeText(context, R.string.fail_show_sinbin, Toast.LENGTH_SHORT).show(); return;}
        inflater.inflate(R.layout.sinbin, this, true);
        this.sinbin = sinbin;

        timer = findViewById(R.id.timer);
        timer.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, Main.vh10);
        main.addOnTouch(this);
        main.addOnTouch(timer);

        if(isHome){
            timer.setGravity(Gravity.LEFT);
        }else{
            timer.setGravity(Gravity.RIGHT);
        }
        this.setBackgroundColor(col);
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
            timer.setTextColor(Color.RED);
            Main.beep();
        }
        String tmp = Main.prettyTimer(remaining);
        timer.setText(tmp);
    }
}
