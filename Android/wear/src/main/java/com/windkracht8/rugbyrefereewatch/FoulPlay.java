package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FoulPlay extends LinearLayout{
    private final TextView player;
    public FoulPlay(Context context, AttributeSet attrs){
        super(context, attrs);
        ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.foulplay, this, true);
        player = findViewById(R.id.foulPlay_player);
    }
    void onCreateMain(Main main){
        findViewById(R.id.card_yellow).setOnClickListener(v->main.card_yellowClick());
        findViewById(R.id.penaltyTry).setOnClickListener(v->main.penaltyTryClick());
        findViewById(R.id.card_red).setOnClickListener(v->main.card_redClick());
    }
    void player(int player_no){
        player.setText(player_no == 0 ? "" : String.valueOf(player_no));
    }
    int player(){
        if(player.getText().length() == 0) return 0;
        return Integer.parseInt(player.getText().toString());
    }
}
