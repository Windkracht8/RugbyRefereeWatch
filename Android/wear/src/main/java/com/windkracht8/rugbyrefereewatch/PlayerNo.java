package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

public class PlayerNo extends ConstraintLayout{
    private final TextView player_no;
    private final TextView b_back;
    private int player_no_int = 0;
    private MatchData.Event event;
    private MatchData.Sinbin sinbin;
    public PlayerNo(@NonNull Context context, @Nullable AttributeSet attrs){
        super(context, attrs);
        ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.player_no, this, true);
        player_no = findViewById(R.id.player_no);
        findViewById(R.id.b_0).setOnClickListener(v->addNumber(0));
        findViewById(R.id.b_1).setOnClickListener(v->addNumber(1));
        findViewById(R.id.b_2).setOnClickListener(v->addNumber(2));
        findViewById(R.id.b_3).setOnClickListener(v->addNumber(3));
        findViewById(R.id.b_4).setOnClickListener(v->addNumber(4));
        findViewById(R.id.b_5).setOnClickListener(v->addNumber(5));
        findViewById(R.id.b_6).setOnClickListener(v->addNumber(6));
        findViewById(R.id.b_7).setOnClickListener(v->addNumber(7));
        findViewById(R.id.b_8).setOnClickListener(v->addNumber(8));
        findViewById(R.id.b_9).setOnClickListener(v->addNumber(9));
        b_back = findViewById(R.id.b_back);
        b_back.setOnClickListener(v->{
            player_no_int = Math.floorDiv(player_no_int, 10);
            showNumber();
        });
    }
    void onCreateMain(Main main){
        findViewById(R.id.b_done).setOnClickListener(v->{
            event.who = player_no_int;
            if(sinbin != null){
                sinbin.who = player_no_int;
                main.updateSinbins();
            }
            setVisibility(View.GONE);
        });
        ((LayoutParams)findViewById(R.id.b_0).getLayoutParams()).setMargins(Main.vh10, 0, Main.vh10, 0);
    }
    void show(MatchData.Event event, MatchData.Sinbin sinbin){
        this.event = event;
        this.sinbin = sinbin;
        player_no_int = 0;
        showNumber();
        setVisibility(View.VISIBLE);
    }
    private void addNumber(int number){
        player_no_int = player_no_int * 10 + number;
        showNumber();
    }
    private void showNumber(){
        if(player_no_int == 0){
            player_no.setText(R.string.no0);
            player_no.setTextColor(getResources().getColor(R.color.hint, null));
            b_back.setTextColor(getResources().getColor(R.color.hint, null));
        }else{
            player_no.setText(String.valueOf(player_no_int));
            player_no.setTextColor(getResources().getColor(R.color.white, null));
            b_back.setTextColor(getResources().getColor(R.color.white, null));
        }
    }
}
