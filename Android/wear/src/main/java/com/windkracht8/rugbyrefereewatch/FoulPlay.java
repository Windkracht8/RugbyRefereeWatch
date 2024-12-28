package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class FoulPlay extends LinearLayout{
    int player_no;

    private final TextView foulPlay_player;
    final ScrollView svPlayerNo;
    private final LinearLayout llPlayerNo;

    public FoulPlay(Context context, AttributeSet attrs){
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        inflater.inflate(R.layout.foulplay, this, true);

        llPlayerNo = findViewById(R.id.llPlayerNo);
        for(int player_no: MatchData.player_nos){
            TextView tmp = new TextView(context, null, 0, R.style.si_item);
            tmp.setText(String.valueOf(player_no));
            tmp.setOnClickListener((v)->onPlayerNoClick(player_no));
            llPlayerNo.addView(tmp);
        }
        svPlayerNo = findViewById(R.id.svPlayerNo);
        foulPlay_player = findViewById(R.id.foulPlay_player);
        foulPlay_player.setOnClickListener((v)-> {
            svPlayerNo.setVisibility(View.VISIBLE);
            svPlayerNo.fullScroll(View.FOCUS_UP);
        });
    }
    void onCreateMain(Main main){
        findViewById(R.id.card_yellow).setOnClickListener(v -> main.card_yellowClick());
        findViewById(R.id.penalty_try).setOnClickListener(v -> main.penalty_tryClick());
        findViewById(R.id.card_red).setOnClickListener(v -> main.card_redClick());
        for(int i = 0; i < llPlayerNo.getChildCount(); i++) main.addOnTouch(llPlayerNo.getChildAt(i));
        if(Main.isScreenRound) main.si_addLayout(svPlayerNo, llPlayerNo);
    }

    void onPlayerNoClick(int player_no){
        this.player_no = player_no;
        String tmp = "#" + player_no;
        foulPlay_player.setText(tmp);
        svPlayerNo.setVisibility(View.GONE);
    }
}
