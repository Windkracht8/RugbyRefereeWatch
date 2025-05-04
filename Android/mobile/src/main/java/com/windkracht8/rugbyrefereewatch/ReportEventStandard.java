package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

class ReportEventStandard extends ReportEvent{
    private final TextView tvLeftTime;
    private final TextView tvMiddle;
    private final TextView tvRightTime;

    ReportEventStandard(Context context, JSONObject event, int period_count, int period_time, int[] score){
        super(context, R.layout.report_event_standard);
        TextView tvLeft = findViewById(R.id.tvLeft);
        tvLeftTime = findViewById(R.id.tvLeftTime);
        tvMiddle = findViewById(R.id.tvMiddle);
        tvRightTime = findViewById(R.id.tvRightTime);
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
                findViewById(R.id.tvReason).setVisibility(View.VISIBLE);
            }else if(what.equals("START")){
                tvMiddle.setText(what_local);
                int width = (getWidth()-tvMiddle.getWidth())/2;
                tvLeft.setWidth(width);
                tvRight.setWidth(width);
            }else{
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

                        boolean isHome = event.getString("team").equals(Main.HOME_ID);
                        int minutes = Math.floorDiv(event.getInt("timer"), 60);
                        String timer = String.valueOf(minutes);
                        if(minutes > (long) period_time * period_count){
                            timer = String.valueOf((period_time * period_count));
                            long over = minutes - ((long) period_time * period_count);
                            timer += "+" + over;
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
    @Override void getFieldWidths(){
        if(TabReport.width_score < tvLeftTime.getWidth()) TabReport.width_score = tvLeftTime.getWidth();
        if(TabReport.width_timer < tvMiddle.getWidth() && tvMiddle.getText().length() < 8) TabReport.width_timer = tvMiddle.getWidth();
        if(TabReport.width_score < tvRightTime.getWidth()) TabReport.width_score = tvRightTime.getWidth();
    }
    @Override void setFieldWidths(){
        tvLeftTime.setWidth(TabReport.width_score);
        if(tvMiddle.getText().length() < 8) tvMiddle.setWidth(TabReport.width_timer);
        tvRightTime.setWidth(TabReport.width_score);
    }
}
