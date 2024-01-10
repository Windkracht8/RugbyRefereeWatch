package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

public class FoulPlay extends LinearLayout{
    public int player_no;
    private Spinner foulPlay_player;
    public FoulPlay(Context context, AttributeSet attrs){
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(inflater == null){Toast.makeText(context, R.string.fail_show_foul_play, Toast.LENGTH_SHORT).show(); return;}
        inflater.inflate(R.layout.foulplay, this, true);
        foulPlay_player = findViewById(R.id.foulPlay_player);
        String[] aPlayerNumbers = new String[] {"0","1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16","17","18","19","20","21","22","23","24","25","26","27","28","29","30","31","32","33","34","35","36","37","38","39","40","41","42","43","44","45","46","47","48","49","50"};
        ArrayAdapter<String> aaPlayerNumbers = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, aPlayerNumbers);
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
    public void onCreateMain(Main main){
        findViewById(R.id.card_yellow).setOnClickListener(v -> main.card_yellowClick());
        findViewById(R.id.penalty_try).setOnClickListener(v -> main.penalty_tryClick());
        findViewById(R.id.card_red).setOnClickListener(v -> main.card_redClick());
    }
    public void setPlayer(int set_player){
        foulPlay_player.setSelection(set_player);
    }
}
