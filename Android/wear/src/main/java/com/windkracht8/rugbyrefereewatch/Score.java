package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

public class Score extends ConstraintLayout{
    MatchData.team team;
    int player_no = 0;

    private final TextView score_player;
    private final TextView score_try;
    private final TextView score_con;
    private final TextView score_goal;
    private final TextView foul_play;
    final ScrollView svPlayerNo;
    private final LinearLayout llPlayerNo;

    public Score(Context context, AttributeSet attrs){
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        inflater.inflate(R.layout.score, this, true);

        score_try = findViewById(R.id.score_try);
        score_con = findViewById(R.id.score_con);
        score_goal = findViewById(R.id.score_goal);
        foul_play = findViewById(R.id.foul_play);

        llPlayerNo = findViewById(R.id.llPlayerNo);
        for(int player_no: MatchData.player_nos){
            TextView tmp = new TextView(context, null, 0, R.style.si_item);
            tmp.setText(String.valueOf(player_no));
            tmp.setOnClickListener((v)->onPlayerNoClick(player_no));
            llPlayerNo.addView(tmp);
        }
        svPlayerNo = findViewById(R.id.svPlayerNo);
        score_player = findViewById(R.id.score_player);
        score_player.setOnClickListener((v)-> {
            svPlayerNo.setVisibility(View.VISIBLE);
            if(player_no == 0) svPlayerNo.fullScroll(View.FOCUS_UP);
        });
    }
    void onCreateMain(Main main){
        score_try.setOnClickListener(v -> main.tryClick());
        score_con.setOnClickListener(v -> main.conversionClick());
        score_goal.setOnClickListener(v -> main.goalClick());
        foul_play.setOnClickListener(v -> main.foulPlayClick());
        for(int i = 0; i < llPlayerNo.getChildCount(); i++) main.addOnTouch(llPlayerNo.getChildAt(i));
        if(Main.isScreenRound){
            main.si_addLayout(svPlayerNo, llPlayerNo);
            foul_play.setPadding(Main.vh25, 0, Main.vh25, Main.vh5);
        }
    }
    void update(){//Thread: UI
        score_try.setVisibility(Main.match.points_try == 0 ? View.GONE : View.VISIBLE);
        score_con.setVisibility(Main.match.points_con == 0 ? View.GONE : View.VISIBLE);
        score_goal.setVisibility(Main.match.points_goal == 0 ? View.GONE : View.VISIBLE);
        score_player.setVisibility(Main.record_player ? View.VISIBLE : View.GONE);
    }
    void clear(){onPlayerNoClick(0);}
    private void onPlayerNoClick(int player_no){
        this.player_no = player_no;
        String tmp = "#" + player_no;
        score_player.setText(tmp);
        svPlayerNo.setVisibility(View.GONE);
    }
}
