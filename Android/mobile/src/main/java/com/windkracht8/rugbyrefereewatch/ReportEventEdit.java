package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

public class ReportEventEdit extends LinearLayout{
    private final Handler handler_message;
    private JSONObject event;
    public ReportEventEdit(Context context){super(context);handler_message=null;}
    public ReportEventEdit(Context context, Handler handler_message, JSONObject event){
        super(context);
        this.handler_message = handler_message;
        this.event = event;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(inflater == null){Toast.makeText(context, R.string.fail_show_match, Toast.LENGTH_SHORT).show(); return;}
        inflater.inflate(R.layout.report_event_edit, this, true);

        Spinner what = findViewById(R.id.what);
        Spinner team = findViewById(R.id.team);

        try{
            TextView timer = findViewById(R.id.timer);
            timer.setText(timerStampToString(event.getLong("timer")));

            switch(event.getString("what")){
                case "TRY":
                    what.setSelection(0);
                    break;
                case "CONVERSION":
                    what.setSelection(1);
                    break;
                case "PENALTY TRY":
                    what.setSelection(2);
                    break;
                case "GOAL":
                    what.setSelection(3);
                    break;
                case "PENALTY GOAL":
                    what.setSelection(4);
                    break;
                case "DROP GOAL":
                    what.setSelection(5);
                    break;
                case "YELLOW CARD":
                    what.setSelection(6);
                    findViewById(R.id.reason).setVisibility(VISIBLE);
                    break;
                case "RED CARD":
                    what.setSelection(7);
                    findViewById(R.id.reason).setVisibility(VISIBLE);
                    break;
                case "PENALTY":
                    what.setSelection(8);
                    break;
                default:
                    setVisibility(GONE);
            }

            if(event.has("team") && event.getString("team").equals("away")){
                team.setSelection(1);
            }
            if(event.has("who")){
                ((EditText)findViewById(R.id.who)).setText(event.getString("who"));
            }
            if(event.has("reason")){
                ((EditText)findViewById(R.id.reason)).setText(event.getString("reason"));
            }
            timer.setLayoutParams(new LinearLayout.LayoutParams(TabReport.timer_edit_width, LinearLayout.LayoutParams.WRAP_CONTENT));
            if(TabReport.what_width>10){
                what.setLayoutParams(new LinearLayout.LayoutParams(TabReport.what_width, LinearLayout.LayoutParams.WRAP_CONTENT));
                team.setLayoutParams(new LinearLayout.LayoutParams(TabReport.team_width, LinearLayout.LayoutParams.WRAP_CONTENT));
            }
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "ReportEventEdit.construct Exception: " + e.getMessage());
            Toast.makeText(getContext(), R.string.fail_show_match, Toast.LENGTH_SHORT).show();
        }
        findViewById(R.id.bDel).setOnClickListener(view -> bDelClick());
    }

    private void bDelClick(){
        try{
            handler_message.sendMessage(handler_message.obtainMessage(Main.MESSAGE_DEL_CLICK, event.getInt("id"), 0));
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "ReportEventEdit.bDelClick Exception: " + e.getMessage());
            Toast.makeText(getContext(), R.string.fail_del_event, Toast.LENGTH_SHORT).show();
        }
    }

    public JSONObject toJson(){
        if(getVisibility() == GONE) return event;
        try{
            String reason = ((EditText)findViewById(R.id.reason)).getText().toString();
            if(event.has("reason") || reason.length() > 0) event.put("reason", reason);

            Spinner what = findViewById(R.id.what);
            switch(what.getSelectedItemPosition()){
                case 0:
                    event.put("what", "TRY");
                    break;
                case 1:
                    event.put("what", "CONVERSION");
                    break;
                case 2:
                    event.put("what", "PENALTY TRY");
                    break;
                case 3:
                    event.put("what", "GOAL");
                    break;
                case 4:
                    event.put("what", "PENALTY GOAL");
                    break;
                case 5:
                    event.put("what", "DROP GOAL");
                    break;
                case 6:
                    event.put("what", "YELLOW CARD");
                    break;
                case 7:
                    event.put("what", "RED CARD");
                    break;
                case 8:
                    event.put("what", "PENALTY");
                    break;
            }

            Spinner team = findViewById(R.id.team);
            event.put("team", team.getSelectedItemPosition() == 0 ? "home" : "away");

            String who = ((EditText)findViewById(R.id.who)).getText().toString();
            if(event.has("who") || who.length() > 0) event.put("who", Integer.parseInt(who));
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "ReportEventEdit.toJson Exception: " + e.getMessage());
            Toast.makeText(getContext(), R.string.fail_save_match, Toast.LENGTH_SHORT).show();
        }
        return event;
    }
    public static String timerStampToString(long timer){
        int temp = (int) (timer / 1000);
        int seconds = (temp % 60);
        int minutes = (temp - seconds) / 60;
        String sTimer = minutes + ":";
        if(seconds < 10) sTimer += "0";
        sTimer += seconds;
        return sTimer;
    }
}
