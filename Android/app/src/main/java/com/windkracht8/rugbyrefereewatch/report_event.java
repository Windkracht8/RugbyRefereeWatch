package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;


public class report_event extends LinearLayout {
    public report_event(Context context){super(context);}
    public report_event(Context context, JSONObject event, int scorewidth) {
        super(context);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater == null) {
            Log.e("report_event", "No inflater");
            return;
        }
        inflater.inflate(R.layout.report_event, this, true);

        TextView tvLeft = findViewById(R.id.tvLeft);
        TextView tvLeftTime = findViewById(R.id.tvLeftTime);
        TextView tvMiddle = findViewById(R.id.tvMiddle);
        TextView tvRightTime = findViewById(R.id.tvRightTime);
        TextView tvRight = findViewById(R.id.tvRight);

        try {
            String text = event.getString("what");
            if(text.startsWith("Start") || text.startsWith("Result")){
                if(text.contains(":")){
                    text = text.substring(0, text.lastIndexOf(" "));
                }
                if(text.startsWith("Result") && event.has("score")) {
                    String[] scores = event.getString("score").split(":");
                    tvLeft.setText(scores[0]);
                    tvRight.setText(scores[1]);
                }
                tvMiddle.setText(text);
                int width = (this.getWidth()-tvMiddle.getWidth())/2;
                tvLeft.setWidth(width);
                tvRight.setWidth(width);
            }else{
                switch(text){
                    case "TRY":
                    case "CONVERSION":
                    case "GOAL":
                        String score = event.getString("score");
                        tvMiddle.setText(score);
                        tvMiddle.setWidth(scorewidth);
                        if(score.length() == 4){
                            //even number of characters will mess up the alignment
                            tvMiddle.setGravity(score.split(":")[0].length() == 2 ? Gravity.START : Gravity.END);
                        }
                    case "YELLOW CARD":
                    case "RED CARD":
                        String timer = event.getString("timer").replace(":", "'");
                        if(event.has("who")) {
                            text += " " + event.getString("who");
                        }

                        String team = event.getString("team");
                        if(team.equals("home")) {
                            tvLeft.setText(text);
                            tvLeftTime.setText(timer);
                        }else{
                            tvRightTime.setText(timer);
                            tvRight.setText(text);
                        }

                        int width = (this.getWidth()-scorewidth)/2-scorewidth;
                        tvLeft.setWidth(width);
                        tvLeftTime.setWidth(scorewidth);
                        tvRightTime.setWidth(scorewidth);
                        tvRight.setWidth(width);
                        break;
                    default:
                        this.setVisibility(View.GONE);
                }
            }

        } catch (Exception e) {
            Log.e("report_event", "report_event: " + e.getMessage());
        }
    }
}
