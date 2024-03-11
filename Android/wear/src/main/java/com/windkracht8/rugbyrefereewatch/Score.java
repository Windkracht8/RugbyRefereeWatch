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

public class Score extends LinearLayout{
    MatchData.team team;
    int player_no;

    private final Spinner score_player;
    private final TextView score_try;
    private final TextView score_con;
    private final TextView score_goal;
    private final TextView foul_play;

    public Score(Context context, AttributeSet attrs){
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        inflater.inflate(R.layout.score, this, true);

        score_player = findViewById(R.id.score_player);
        score_try = findViewById(R.id.score_try);
        score_con = findViewById(R.id.score_con);
        score_goal = findViewById(R.id.score_goal);
        foul_play = findViewById(R.id.foul_play);

        String[] aPlayerNumbers = new String[] {"0","1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16","17","18","19","20","21","22","23","24","25","26","27","28","29","30","31","32","33","34","35","36","37","38","39","40","41","42","43","44","45","46","47","48","49","50"};
        ArrayAdapter<String> aaPlayerNumbers = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, aPlayerNumbers);
        aaPlayerNumbers.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        score_player.setAdapter(aaPlayerNumbers);
        score_player.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id){
                player_no = position;
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView){
                player_no = 0;
            }
        });
        if(!Main.isScreenRound){
            findViewById(R.id.llScore).setPadding(0, 0, 0, 0);
        }
    }
    void onCreateMain(Main main){
        score_try.setOnClickListener(v -> main.tryClick());
        score_con.setOnClickListener(v -> main.conversionClick());
        score_goal.setOnClickListener(v -> main.goalClick());
        foul_play.setOnClickListener(v -> main.foulPlayClick());
    }
    void update(){//Thread: Always on UI thread
        score_try.setVisibility(Main.match.points_try == 0 ? View.GONE : View.VISIBLE);
        score_con.setVisibility(Main.match.points_con == 0 ? View.GONE : View.VISIBLE);
        score_goal.setVisibility(Main.match.points_goal == 0 ? View.GONE : View.VISIBLE);
        score_player.setVisibility(Main.record_player ? View.VISIBLE : View.GONE);
        if(Main.isScreenRound){
            foul_play.setPadding(Main.vh25, 0, Main.vh25, 0);
        }
    }
    void clear(){score_player.setSelection(0);}
}
