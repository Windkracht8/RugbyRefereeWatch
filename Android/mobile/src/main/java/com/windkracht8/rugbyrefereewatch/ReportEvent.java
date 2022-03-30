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


public class ReportEvent extends LinearLayout {
    public ReportEvent(Context context){super(context);}
    public ReportEvent(Context context, JSONObject event){
        super(context);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(inflater == null){Toast.makeText(context, "Failed to show match", Toast.LENGTH_SHORT).show(); return;}
        inflater.inflate(R.layout.report_event, this, true);

        TextView tvLeft = findViewById(R.id.tvLeft);
        TextView tvLeftTime = findViewById(R.id.tvLeftTime);
        TextView tvMiddle = findViewById(R.id.tvMiddle);
        TextView tvRightTime = findViewById(R.id.tvRightTime);
        TextView tvRight = findViewById(R.id.tvRight);

        try{
            String text = event.getString("what");
            if(text.startsWith("Start") || text.startsWith("Result")){
                if(text.contains(":")){
                    text = text.substring(0, text.lastIndexOf(" "));
                }
                if(text.startsWith("Result") && event.has("score")){
                    String[] scores = event.getString("score").split(":");
                    tvLeft.setText(scores[0]);
                    tvRight.setText(scores[1]);
                }
                tvMiddle.setText(text);
                int width = (this.getWidth()-tvMiddle.getWidth())/2;
                tvLeft.setWidth(width);
                tvRight.setWidth(width);
            }else{
                tvMiddle.setWidth(TabReport.score_width);
                switch(text){
                    case "TRY":
                    case "CONVERSION":
                    case "PENALTY TRY":
                    case "GOAL":
                    case "PENALTY GOAL":
                    case "DROP GOAL":
                        String score = event.getString("score");
                        tvMiddle.setText(score);
                        if(score.length() == 4){
                            //even number of characters will mess up the alignment
                            tvMiddle.setGravity(score.split(":")[0].length() == 2 ? Gravity.START : Gravity.END);
                        }
                    case "YELLOW CARD":
                    case "RED CARD":
                        String timer = event.getString("timer");
                        timer = timer.substring(0, timer.length()-3) + "'";
                        if(event.has("who")){
                            text += " " + event.getString("who");
                        }

                        String team = event.getString("team");
                        if(team.equals("home")){
                            tvLeft.setText(text);
                            tvLeftTime.setText(timer);
                        }else{
                            tvRightTime.setText(timer);
                            tvRight.setText(text);
                        }

                        int width = (this.getWidth()-TabReport.score_width)/2-TabReport.timer_width;
                        tvLeft.setWidth(width);
                        tvLeftTime.setWidth(TabReport.timer_width);
                        tvRightTime.setWidth(TabReport.timer_width);
                        tvRight.setWidth(width);
                        if(event.has("reason")){
                            ((TextView)findViewById(R.id.tvReason)).setText(event.getString("reason"));
                            findViewById(R.id.tvReason).setVisibility(View.VISIBLE);
                        }
                        break;
                    default:
                        this.setVisibility(View.GONE);
                }
            }
        }catch(Exception e){
            Log.e("ReportEvent", "ReportEvent: " + e.getMessage());
            Toast.makeText(getContext(), "Failed to show match", Toast.LENGTH_SHORT).show();
        }
    }
}
