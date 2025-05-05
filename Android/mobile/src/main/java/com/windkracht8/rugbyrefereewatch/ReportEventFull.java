/*
 * Copyright 2020-2025 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

class ReportEventFull extends ReportEvent{
    private TextView tvTime;
    private TextView tvTimer;
    ReportEventFull(Context context, JSONObject event, JSONObject match, int period_count, int period_time){
        super(context, R.layout.report_event_full);
        try{
            tvTime = findViewById(R.id.tvTime);
            tvTime.setText(event.getString("time"));

            tvTimer = findViewById(R.id.tvTimer);

            int seconds = event.getInt("timer");
            int minutes = Math.floorDiv(seconds, 60);
            seconds %= 60;
            String timer = String.valueOf(minutes);
            if(minutes > (long) period_time * period_count){
                timer = String.valueOf((period_time * period_count));
                long over = minutes - ((long) period_time * period_count);
                timer += "+" + over;
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
                String score = event.getString("score");
                tvScore.setText(score);
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
    @Override void getFieldWidths(){
        if(tvTime != null && TabReport.width_time < tvTime.getWidth()) TabReport.width_time = tvTime.getWidth();
        if(tvTimer != null && TabReport.width_timer < tvTimer.getWidth()) TabReport.width_timer = tvTimer.getWidth();
    }
    @Override void setFieldWidths(){
        if(tvTime != null) tvTime.setWidth(TabReport.width_time);
        if(tvTimer != null) tvTimer.setWidth(TabReport.width_timer);
    }
}
