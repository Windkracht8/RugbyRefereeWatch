package com.windkracht8.rugbyreferee;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

public class tlMatch extends LinearLayout {
    private TextView tvHomeName;
    private TextView tvAwayName;
    private TextView tvHomeTrys;
    private TextView tvAwayTrys;
    private TextView tvHomeCons;
    private TextView tvAwayCons;
    private TextView tvHomeGoals;
    private TextView tvAwayGoals;
    private TextView tvHomeTot;
    private TextView tvAwayTot;
    private TextView tvEvents;

    public tlMatch(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater != null) {
            inflater.inflate(R.layout.tlmatch, this, true);
        }

        tvHomeName = findViewById(R.id.tvHomeName);
        tvAwayName = findViewById(R.id.tvAwayName);
        tvHomeTrys = findViewById(R.id.tvHomeTrys);
        tvAwayTrys = findViewById(R.id.tvAwayTrys);
        tvHomeCons = findViewById(R.id.tvHomeCons);
        tvAwayCons = findViewById(R.id.tvAwayCons);
        tvHomeGoals = findViewById(R.id.tvHomeGoals);
        tvAwayGoals = findViewById(R.id.tvAwayGoals);
        tvHomeTot = findViewById(R.id.tvHomeTot);
        tvAwayTot = findViewById(R.id.tvAwayTot);
        tvEvents = findViewById(R.id.tvEvents);
    }

    public void gotMatch(final JSONObject match){
        try {
            JSONObject home = match.getJSONObject("home");
            JSONObject away = match.getJSONObject("away");

            String homeName = home.getString("team") + " (" +
                    home.getString("color") + ")";
            tvHomeName.setText(homeName);
            String awayName = away.getString("team") + " (" +
                    away.getString("color") + ")";
            tvAwayName.setText(awayName);
            tvHomeTrys.setText(home.getString("trys"));
            tvAwayTrys.setText(away.getString("trys"));
            tvHomeCons.setText(home.getString("cons"));
            tvAwayCons.setText(away.getString("cons"));
            tvHomeGoals.setText(home.getString("goals"));
            tvAwayGoals.setText(away.getString("goals"));
            tvHomeTot.setText(home.getString("tot"));
            tvAwayTot.setText(away.getString("tot"));

            StringBuilder sbEvents = new StringBuilder();
            JSONArray events = match.getJSONArray("events");
            for (int i = 0; i < events.length(); i++) {
                JSONObject event = events.getJSONObject(i);
                sbEvents.append(event.getString("time")).append(" ");
                sbEvents.append(event.getString("timer")).append(" ");
                sbEvents.append(event.getString("what"));
                if(event.has("team")) {
                    sbEvents.append(" ").append(event.getString("team"));
                }
                sbEvents.append("\n");
            }
            tvEvents.setText(sbEvents.toString());
            this.setVisibility(View.VISIBLE);
        }catch (Exception e){
            Log.e("tlMatch", "json error: " + e.getMessage());
        }
    }
}
