package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;


public class report_event extends LinearLayout {
    public report_event(Context context){super(context);}
    public report_event(Context context, JSONObject event, JSONObject match, int scorewidth) {
        super(context);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater == null) {
            Log.e("report_event", "No inflater");
            return;
        }
        inflater.inflate(R.layout.report_event, this, true);

        try {
            String what = event.getString("what");
            if(what.startsWith("Start") || what.startsWith("Result")){
                //TODO: remove score from string what
                TextView tvAll = findViewById(R.id.tvAll);
                String text = what;
                if(event.has("score")) {
                    String[] scores = event.getString("score").split(":");
                    text = scores[0] + " " + text + " " + scores[1];
                }
                tvAll.setText(text);
                tvAll.setVisibility(android.view.View.VISIBLE);
            }else{
                switch(what){
                    case "TRY":
                    case "CONVERSION":
                    case "GOAL":
                        TextView tvScore = findViewById(R.id.tvScore);
                        tvScore.setText(event.getString("score"));
                        tvScore.setVisibility(android.view.View.VISIBLE);
                    case "YELLOW CARD":
                    case "RED CARD":
                        //show item for the team
                        //time + what + who
                        String team = event.getString("team");
                        TextView tvTeam;
                        if(team == "home"){
                            tvTeam = findViewById(R.id.tvHome);
                        }else{
                            tvTeam = findViewById(R.id.tvAway);
                        }
                        String text = event.getString("timer").replace(":", "'");
                        text += " " + what;
                        if(event.has("who")) {
                            text += " " + event.getString("who");
                        }

                        tvTeam.setText(text);
                        tvTeam.setVisibility(android.view.View.VISIBLE);
                        break;
                }
            }

            TextView tvWhat = findViewById(R.id.tvWhat);
            tvWhat.setText(event.getString("what"));



        } catch (Exception e) {
            Log.e("report_event", "report_event: " + e.getMessage());
        }
    }
}
