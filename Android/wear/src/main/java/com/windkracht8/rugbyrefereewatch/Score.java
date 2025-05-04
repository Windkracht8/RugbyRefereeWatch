package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Score extends LinearLayout{
    MatchData.Team team;

    private final TextView score_try;
    private final TextView score_con;
    private final TextView score_goal;
    private final TextView foul_play;

    public Score(Context context, AttributeSet attrs){
        super(context, attrs);
        ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.score, this, true);
        score_try = findViewById(R.id.score_try);
        score_con = findViewById(R.id.score_con);
        score_goal = findViewById(R.id.score_goal);
        foul_play = findViewById(R.id.foul_play);
    }
    void onCreateMain(Main main){
        score_try.setOnClickListener(v->main.tryClick());
        score_con.setOnClickListener(v->main.conversionClick());
        score_goal.setOnClickListener(v->main.goalClick());
        foul_play.setOnClickListener(v->main.foulPlayClick());
        if(Main.isScreenRound) foul_play.setPadding(Main.vh25, 0, Main.vh25, Main.vh5);
    }
    void update(){//Thread: UI
        score_try.setVisibility(Main.match.points_try == 0 ? View.GONE : View.VISIBLE);
        score_con.setVisibility(Main.match.points_con == 0 ? View.GONE : View.VISIBLE);
        score_goal.setVisibility(Main.match.points_goal == 0 ? View.GONE : View.VISIBLE);
    }
}
