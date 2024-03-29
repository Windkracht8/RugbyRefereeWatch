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

class ReportEvent extends LinearLayout{
    ReportEvent(Context context, JSONObject event, int period_count, int period_time, int[] score){
        super(context);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        inflater.inflate(R.layout.report_event, this, true);

        TextView tvLeft = findViewById(R.id.tvLeft);
        TextView tvLeftTime = findViewById(R.id.tvLeftTime);
        TextView tvMiddle = findViewById(R.id.tvMiddle);
        TextView tvRightTime = findViewById(R.id.tvRightTime);
        TextView tvRight = findViewById(R.id.tvRight);

        try{
            String what = event.getString("what");
            int period = event.getInt("period");
            String what_local = Translator.getEventTypeLocal(context, what, period, period_count);
            if(what.equals("END")){
                tvLeft.setText(String.valueOf(score[0]));
                tvRight.setText(String.valueOf(score[1]));
                tvMiddle.setText(what_local);
                int width = (getWidth() - tvMiddle.getWidth()) / 2;
                tvLeft.setWidth(width);
                tvRight.setWidth(width);
            }else if(what.equals("START")){
                tvMiddle.setText(what_local);
                int width = (getWidth()-tvMiddle.getWidth())/2;
                tvLeft.setWidth(width);
                tvRight.setWidth(width);
            }else{
                tvMiddle.setWidth(TabReport.score_width);
                switch(what){
                    case "TRY":
                    case "CONVERSION":
                    case "PENALTY TRY":
                    case "GOAL":
                    case "PENALTY GOAL":
                    case "DROP GOAL":
                        String sScore = score[0] + ":" + score[1];
                        tvMiddle.setText(sScore);
                        if(sScore.length() == 4){
                            //even number of characters will mess up the alignment
                            tvMiddle.setGravity(score[0] >= 10 ? Gravity.START : Gravity.END);
                        }
                    case "YELLOW CARD":
                    case "RED CARD":
                        if(event.has("who")) what_local += " " + event.getString("who");

                        boolean isHome = event.getString("team").equals("home");
                        long minutes = event.getLong("timer") / 60000;
                        String timer = Long.toString(minutes);
                        if(minutes > (long) period_time * period_count){
                            timer = String.valueOf((period_time * period_count));
                            long over = minutes - ((long) period_time * period_count);
                            timer += "+" + over;
                            if(over > 9){
                                tvLeftTime.setWidth((int) (TabReport.timer_width * 2.5));
                                tvRightTime.setWidth((int) (TabReport.timer_width * 2.5));
                            }else{
                                tvLeftTime.setWidth(TabReport.timer_width * 2);
                                tvRightTime.setWidth(TabReport.timer_width * 2);
                            }
                        }else{
                            tvLeftTime.setWidth(TabReport.timer_width);
                            tvRightTime.setWidth(TabReport.timer_width);
                        }
                        timer += "'";
                        if(isHome){
                            tvLeft.setText(what_local);
                            tvLeftTime.setText(timer);
                        }else{
                            tvRightTime.setText(timer);
                            tvRight.setText(what_local);
                        }
                        if(event.has("reason")){
                            TextView tvReason = findViewById(R.id.tvReason);
                            tvReason.setText(event.getString("reason"));
                            tvReason.setVisibility(View.VISIBLE);
                            tvReason.setGravity(isHome ? Gravity.START : Gravity.END);
                        }
                        break;
                    default:
                        setVisibility(View.GONE);
                }
            }
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "ReportEvent.construct Exception: " + e.getMessage());
            Toast.makeText(getContext(), R.string.fail_show_match, Toast.LENGTH_SHORT).show();
        }
    }
}
