package com.windkracht8.rugbyrefereewatch;

import android.graphics.Color;
import android.view.Gravity;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.widget.TextViewCompat;

import java.util.Set;

class Sinbin extends AppCompatTextView{
    private final static Set<String> colors_not_red = Set.of("brown", "orange", "red");
    final MatchData.Sinbin sinbin;
    private final Main main;
    Sinbin(Main main, MatchData.Sinbin sinbin, int color){
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
        int remaining = sinbin.end - Main.getDurationFull();
        if(remaining < -60){
            sinbin.hide = true;
        }
        if(sinbin.ended){
            return;
        }
        if(remaining <= 0){
            remaining = 0;
            sinbin.ended = true;
            if((sinbin.team_is_home && !colors_not_red.contains(Main.match.home.color)) ||
                    (!sinbin.team_is_home && !colors_not_red.contains(Main.match.away.color))
            ){
                setTextColor(Color.RED);
            }
            main.beep(main.getString(R.string.ended, main.getString(R.string.sinbin)));
        }
        String tmp = Utils.prettyTimer(remaining);
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
