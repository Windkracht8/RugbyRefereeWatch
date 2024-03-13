package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

class ReportEventFull extends LinearLayout{
    ReportEventFull(Context context, JSONObject event, JSONObject match, int period_count, int period_time){
        super(context);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        inflater.inflate(R.layout.report_event_full, this, true);

        try{
            TextView tvTime = findViewById(R.id.tvTime);
            tvTime.setWidth(TabReport.time_width);
            tvTime.setText(event.getString("time"));

            TextView tvTimer = findViewById(R.id.tvTimer);
            tvTimer.setWidth(TabReport.score_width);

            int temp = (int) (event.getLong("timer") / 1000);
            int seconds = (temp % 60);
            int minutes = (temp - seconds) / 60;
            String timer = Long.toString(minutes);
            if(minutes > (long) period_time * period_count){
                timer = String.valueOf((period_time * period_count));
                long over = minutes - ((long) period_time * period_count);
                timer += "+" + over;
                if(over > 9){
                    tvTimer.setWidth((int) (TabReport.score_width * 1.7));
                }else{
                    tvTimer.setWidth((int) (TabReport.score_width * 1.5));
                }
            }
            timer += "'";
            if(seconds < 10) timer += "0";
            timer += seconds;
            tvTimer.setText(timer);

            TextView tvWhat = findViewById(R.id.tvWhat);
            int period = event.getInt("period");
            tvWhat.setText(Translator.getEventTypeLocal(context, event.getString("what"), period, period_count));
            if(event.has("team")){
                TextView tvTeam = findViewById(R.id.tvTeam);
                String team = event.getString("team");
                team = match.has(team) ? Main.getTeamName(context, match.getJSONObject(team)) : team;
                tvTeam.setText(team);
            }
            if(event.has("who")){
                TextView tvWho = findViewById(R.id.tvWho);
                tvWho.setText(event.getString("who"));
            }
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
            if(event.has("reason")){
                TextView tvReason = findViewById(R.id.tvReason);
                tvReason.setText(event.getString("reason"));
                tvReason.setVisibility(View.VISIBLE);
            }
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "ReportEventFull.construct Exception: " + e.getMessage());
            Toast.makeText(context, R.string.fail_show_match, Toast.LENGTH_SHORT).show();
        }
    }
}
