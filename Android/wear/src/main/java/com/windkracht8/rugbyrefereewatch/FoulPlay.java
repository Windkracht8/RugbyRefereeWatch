package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;

public class FoulPlay extends LinearLayout{
    int player_no;
    private final Spinner foulPlay_player;
    public FoulPlay(Context context, AttributeSet attrs){
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        inflater.inflate(R.layout.foulplay, this, true);
        foulPlay_player = findViewById(R.id.foulPlay_player);
        ArrayAdapter<String> aaPlayerNumbers = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, Score.aPlayerNumbers);
        aaPlayerNumbers.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        foulPlay_player.setAdapter(aaPlayerNumbers);
        foulPlay_player.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id){
                player_no = position;
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView){
                player_no = 0;
            }
        });
    }
    void onCreateMain(Main main){
        findViewById(R.id.card_yellow).setOnClickListener(v -> main.card_yellowClick());
        findViewById(R.id.penalty_try).setOnClickListener(v -> main.penalty_tryClick());
        findViewById(R.id.card_red).setOnClickListener(v -> main.card_redClick());
    }
    void setPlayer(int set_player){foulPlay_player.setSelection(set_player);}
}
