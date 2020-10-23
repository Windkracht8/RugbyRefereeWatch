package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;


public class report_event extends LinearLayout {
    public report_event(Context context){super(context);}
    public report_event(Context context, JSONObject event, JSONObject match, int timewidth, int timerwidth, int scorewidth) {
        super(context);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater != null) {
            inflater.inflate(R.layout.report_event, this, true);
        }

        try {
            TextView tvTime = findViewById(R.id.tvTime);
            tvTime.setWidth(timewidth);
            tvTime.setText(event.getString("time"));

            TextView tvTimer = findViewById(R.id.tvTimer);
            tvTimer.setWidth(timerwidth);
            tvTimer.setText(event.getString("timer"));

            if(event.has("score")) {
                TextView tvScore = findViewById(R.id.tvScore);
                tvScore.setWidth(scorewidth);
                tvScore.setText(event.getString("score"));
            }

            TextView tvWhat = findViewById(R.id.tvWhat);
            tvWhat.setText(event.getString("what"));

            if(event.has("team")) {
                TextView tvTeam = findViewById(R.id.tvTeam);
                String team = event.getString("team");
                team = match.has(team) ? MainActivity.getTeamName(match.getJSONObject(team)) : team;
                tvTeam.setText(team);
            }

            if(event.has("who")) {
                TextView tvWho = findViewById(R.id.tvWho);
                tvWho.setText(event.getString("who"));
            }
        } catch (Exception e) {
            Log.e("report_event", "report_event: " + e.getMessage());
        }
    }
}
