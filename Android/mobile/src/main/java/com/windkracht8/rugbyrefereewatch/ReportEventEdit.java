package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;


public class ReportEventEdit extends LinearLayout{
    private JSONObject event;
    public ReportEventEdit(Context context){super(context);}
    public ReportEventEdit(Context context, JSONObject event){
        super(context);
        this.event = event;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(inflater == null){Toast.makeText(context, context.getString(R.string.fail_show_match), Toast.LENGTH_SHORT).show(); return;}
        inflater.inflate(R.layout.report_event_edit, this, true);

        Spinner what = findViewById(R.id.what);
        String[] a = new String[] {context.getString(R.string.TRY),
                                context.getString(R.string.CONVERSION),
                                context.getString(R.string.PENALTY_TRY),
                                context.getString(R.string.GOAL),
                                context.getString(R.string.PENALTY_GOAL),
                                context.getString(R.string.DROP_GOAL),
                                context.getString(R.string.YELLOW_CARD),
                                context.getString(R.string.RED_CARD),
                                context.getString(R.string.PENALTY)
        };
        ArrayAdapter<String> aa = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, a);
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        what.setAdapter(aa);

        Spinner team = findViewById(R.id.team);
        a = new String[] {context.getString(R.string.HOME),context.getString(R.string.AWAY)};
        aa = new ArrayAdapter<>(team.getContext(), android.R.layout.simple_spinner_item, a);
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        team.setAdapter(aa);

        try{
            TextView timer = findViewById(R.id.timer);
            timer.setText(event.getString("timer"));

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
                    this.setVisibility(GONE);
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
            Log.e(MainActivity.RRW_LOG_TAG, "ReportEventEdit.construct Exception: " + e.getMessage());
            Toast.makeText(getContext(), context.getString(R.string.fail_show_match), Toast.LENGTH_SHORT).show();
        }
        findViewById(R.id.bDel).setOnClickListener(view -> bDelClick());
    }

    public void bDelClick(){
        try{
            Intent intent = new Intent("com.windkracht8.rugbyrefereewatch");
            intent.putExtra("intent_type", "bDelClick");
            intent.putExtra("event_id", event.getInt("id"));
            getContext().sendBroadcast(intent);
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "ReportEventEdit.bDelClick Exception: " + e.getMessage());
            Toast.makeText(getContext(), getContext().getString(R.string.fail_del_event), Toast.LENGTH_SHORT).show();
        }
    }

    public JSONObject toJson(){
        if(this.getVisibility() == GONE) return event;
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
            Log.e(MainActivity.RRW_LOG_TAG, "ReportEventEdit.toJson Exception: " + e.getMessage());
            Toast.makeText(getContext(), getContext().getString(R.string.fail_save_match), Toast.LENGTH_SHORT).show();
        }
        return event;
    }
}
