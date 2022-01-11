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

public class Score extends LinearLayout {
    public MatchData.team team;
    public int player_no;

    private final Spinner player;
    private final TextView score_try;
    private final TextView score_con;
    private final TextView score_goal;
    private final TextView cards;

    public Score(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        inflater.inflate(R.layout.score, this, true);

        player = findViewById(R.id.player);
        score_try = findViewById(R.id.score_try);
        score_con = findViewById(R.id.score_con);
        score_goal = findViewById(R.id.score_goal);
        cards = findViewById(R.id.cards);

        String[] aPlayerNumbers = new String[] {"0","1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16","17","18","19","20","21","22","23","24","25","26","27","28","29","30","31","32","33","34","35","36","37","38","39","40","41","42","43","44","45","46","47","48","49","50"};
        ArrayAdapter<String> aaPlayerNumbers = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, aPlayerNumbers);
        aaPlayerNumbers.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        player.setAdapter(aaPlayerNumbers);
        player.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                player_no = position;
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                player_no = 0;
            }
        });
    }
    public void load(MatchData.team team){
        this.team = team;
    }
    public void update(MatchData match){
        score_try.setVisibility(match.points_try == 0 ? View.GONE : View.VISIBLE);
        score_con.setVisibility(match.points_con == 0 ? View.GONE : View.VISIBLE);
        score_goal.setVisibility(match.points_goal == 0 ? View.GONE : View.VISIBLE);

        int height;
        if(MainActivity.record_player){
            findViewById(R.id.player_wrap).setVisibility(View.VISIBLE);
            height = MainActivity.vh18;
        }else{
            findViewById(R.id.player_wrap).setVisibility(View.GONE);
            height = MainActivity.vh25;
        }
        ((TextView)findViewById(R.id.player_label)).setHeight(height);
        score_try.setHeight(height);
        score_con.setHeight(height);
        score_goal.setHeight(height);
        cards.setHeight(height);
    }
    public void clear(){
        player.setSelection(0);
    }

}
