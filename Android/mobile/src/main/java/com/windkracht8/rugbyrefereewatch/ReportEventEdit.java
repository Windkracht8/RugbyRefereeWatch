package com.windkracht8.rugbyrefereewatch;

import android.util.Log;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

class ReportEventEdit extends ReportEvent{
    private final Main main;
    private final JSONObject event;
    private final TextView timer;
    private final Spinner what;
    private final Spinner team;

    ReportEventEdit(Main main, JSONObject event){
        super(main, R.layout.report_event_edit);
        this.main = main;
        this.event = event;
        what = findViewById(R.id.what);
        team = findViewById(R.id.team);

        timer = findViewById(R.id.timer);
        try{
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
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "ReportEventEdit.construct Exception: " + e.getMessage());
            Toast.makeText(getContext(), R.string.fail_show_match, Toast.LENGTH_SHORT).show();
        }
        findViewById(R.id.bDel).setOnClickListener(v->bDelClick());
    }
    @Override void getFieldWidths(){
        if(TabReport.width_timer < timer.getWidth()) TabReport.width_timer = timer.getWidth();
        if(TabReport.width_what < what.getWidth()) TabReport.width_what = what.getWidth();
        if(TabReport.width_team < team.getWidth()) TabReport.width_team = team.getWidth();
    }
    @Override void setFieldWidths(){
        timer.setWidth(TabReport.width_timer);
        what.setMinimumWidth(TabReport.width_what);
        team.setMinimumWidth(TabReport.width_team);
    }

    private void bDelClick(){
        try{
            main.tabReport.bDelClick(event.getInt("id"));
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "ReportEventEdit.bDelClick Exception: " + e.getMessage());
            Toast.makeText(getContext(), R.string.fail_del_event, Toast.LENGTH_SHORT).show();
        }
    }

    JSONObject toJson(){
        if(getVisibility() == GONE) return event;
        try{
            String reason = ((EditText)findViewById(R.id.reason)).getText().toString();
            if(event.has("reason") || !reason.isEmpty()) event.put("reason", reason);

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
            if(!who.isEmpty()){
                event.put("who", Integer.parseInt(who));
            }else if(event.has("who")){
                event.remove("who");
            }
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "ReportEventEdit.toJson Exception: " + e.getMessage());
            Toast.makeText(getContext(), R.string.fail_save_match, Toast.LENGTH_SHORT).show();
        }
        return event;
    }
    static String timerStampToString(long timer){
        int temp = (int) (timer / 1000);
        int seconds = (temp % 60);
        int minutes = (temp - seconds) / 60;
        String sTimer = minutes + ":";
        if(seconds < 10) sTimer += "0";
        sTimer += seconds;
        return sTimer;
    }
}
