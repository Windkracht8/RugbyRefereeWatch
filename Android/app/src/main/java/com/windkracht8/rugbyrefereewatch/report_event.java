package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;


public class report_event extends LinearLayout{
    public report_event(Context context){super(context);}
    public report_event(Context context, JSONObject event, JSONObject match) {
        super(context);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater != null) {
            inflater.inflate(R.layout.report_event, this, true);
        }

        try {
            TextView tvTime = findViewById(R.id.tvTime);
            tvTime.setText(event.getString("time"));

            TextView tvTimer = findViewById(R.id.tvTimer);
            tvTimer.setText(event.getString("timer"));

            TextView tvWhat = findViewById(R.id.tvWhat);
            String sWhat = event.getString("what");

            if(event.has("team")) {
                String team = event.getString("team");
                sWhat += " ";
                sWhat += match.has(team) ? MainActivity.getTeamName(match.getJSONObject(team)) : team;
                if(event.has("who")) {
                    sWhat += " " + event.getString("who");
                }
            }
            tvWhat.setText(sWhat);
        } catch (Exception e) {
            Log.e("report_event", "report_event: " + e.getMessage());
        }
    }
}
