package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class FoulPlay extends LinearLayout{
    public int player_no;

    private Spinner player;
    private TextView card_yellow;
    private TextView penalty_try;
    private TextView card_red;

    public FoulPlay(Context context, AttributeSet attrs){
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(inflater == null){Toast.makeText(context, "Failed to show foul play screen", Toast.LENGTH_SHORT).show(); return;}
        inflater.inflate(R.layout.foulplay, this, true);

        player = findViewById(R.id.player);
        card_yellow = findViewById(R.id.card_yellow);
        penalty_try = findViewById(R.id.penalty_try);
        card_red = findViewById(R.id.card_red);

        String[] aPlayerNumbers = new String[] {"0","1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16","17","18","19","20","21","22","23","24","25","26","27","28","29","30","31","32","33","34","35","36","37","38","39","40","41","42","43","44","45","46","47","48","49","50"};
        ArrayAdapter<String> aaPlayerNumbers = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, aPlayerNumbers);
        aaPlayerNumbers.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        player.setAdapter(aaPlayerNumbers);
        player.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
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

    public void update(){
        int height;
        if(MainActivity.record_player){
            findViewById(R.id.player_wrap).setVisibility(View.VISIBLE);
            height = MainActivity.vh25;
            ((TextView)findViewById(R.id.player_label)).setHeight(height);
        }else{
            findViewById(R.id.player_wrap).setVisibility(View.INVISIBLE);
            ((TextView)findViewById(R.id.player_label)).setHeight(MainActivity.vh15);
            height = MainActivity.vh30;
        }

        card_yellow.setHeight(height);
        penalty_try.setHeight(height);
        card_red.setHeight(height);
    }
    public void setPlayer(int set_player){
        player.setSelection(set_player);
    }
}
