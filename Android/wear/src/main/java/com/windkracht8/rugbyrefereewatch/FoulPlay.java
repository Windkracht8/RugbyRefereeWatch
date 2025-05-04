package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

public class FoulPlay extends LinearLayout{
    public FoulPlay(Context context, AttributeSet attrs){
        super(context, attrs);
        ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.foulplay, this, true);
    }
    void onCreateMain(Main main){
        findViewById(R.id.card_yellow).setOnClickListener(v->main.card_yellowClick());
        findViewById(R.id.penaltyTry).setOnClickListener(v->main.penaltyTryClick());
        findViewById(R.id.card_red).setOnClickListener(v->main.card_redClick());
    }
}
