package com.windkracht8.rugbyreferee;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

public class report extends LinearLayout {
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
    private TableLayout tlEvents;

    public report(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater != null) {
            inflater.inflate(R.layout.report, this, true);
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
        tlEvents = findViewById(R.id.tlEvents);
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

            if(tlEvents.getChildCount() > 0)
                tlEvents.removeAllViews();

            JSONArray events = match.getJSONArray("events");
            Context context = getContext();
            for (int i = 0; i < events.length(); i++) {
                JSONObject event = events.getJSONObject(i);

                TableRow row = new TableRow(context);
                TextView time = new TextView(context);
                if(event.has("time")) {
                    time.setText(event.getString("time"));
                }
                row.addView(time);

                TextView timer = new TextView(context);
                timer.setGravity(Gravity.END);
                timer.setPadding(20,0,20,0);
                if(event.has("timer")) {
                    timer.setText(event.getString("timer"));
                }
                row.addView(timer);

                TextView what = new TextView(context);
                String sWhat = "";
                if(event.has("what")) {
                    sWhat += event.getString("what") + " ";
                }
                if(event.has("team")) {
                    sWhat += event.getString("team") + " ";
                }
                if(event.has("who")) {
                    sWhat += event.getString("who") + " ";
                }
                what.setText(sWhat);
                row.addView(what);
                row.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                tlEvents.addView(row);
            }

        }catch (Exception e){
            Log.e("tlMatch", "json error: " + e.getMessage());
        }
    }
}
