package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;


public class ReportEventFull extends LinearLayout{
    public ReportEventFull(Context context){super(context);}
    public ReportEventFull(Context context, JSONObject event, JSONObject match, int period_count){
        super(context);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(inflater == null){Toast.makeText(context, R.string.fail_show_match, Toast.LENGTH_SHORT).show(); return;}
        inflater.inflate(R.layout.report_event_full, this, true);

        try{
            TextView tvTime = findViewById(R.id.tvTime);
            tvTime.setWidth(TabReport.time_width);
            tvTime.setText(event.getString("time"));

            TextView tvTimer = findViewById(R.id.tvTimer);
            tvTimer.setWidth(TabReport.score_width);
            String timer = event.getString("timer").replace(":", "'");
            tvTimer.setText(timer);

            if(event.has("score")){
                TextView tvScore = findViewById(R.id.tvScore);
                tvScore.setWidth(TabReport.score_width);
                String score = event.getString("score");
                tvScore.setText(score);
                if(score.length() == 4){
                    //even number of characters will mess up the alignment
                    tvScore.setGravity(score.split(":")[0].length() == 2 ? Gravity.START : Gravity.END);
                }
            }

            TextView tvWhat = findViewById(R.id.tvWhat);
            int period = event.getInt("period");
            tvWhat.setText(translator.getEventTypeLocal(context, event.getString("what"), period, period_count));

            if(event.has("team")){
                TextView tvTeam = findViewById(R.id.tvTeam);
                String team = event.getString("team");
                team = match.has(team) ? MainActivity.getTeamName(context, match.getJSONObject(team)) : team;
                tvTeam.setText(team);
            }

            if(event.has("who")){
                TextView tvWho = findViewById(R.id.tvWho);
                tvWho.setText(event.getString("who"));
            }
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "ReportEventFull.construct Exception: " + e.getMessage());
            Toast.makeText(context, R.string.fail_show_match, Toast.LENGTH_SHORT).show();
        }
    }
}
