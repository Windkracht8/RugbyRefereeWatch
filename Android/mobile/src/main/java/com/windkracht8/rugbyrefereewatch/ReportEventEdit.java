/*
 * Copyright 2020-2025 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
    private final Spinner team;

    ReportEventEdit(Main main, JSONObject event){
        super(main, R.layout.report_event_edit);
        this.main = main;
        this.event = event;
        Spinner what = findViewById(R.id.what);
        team = findViewById(R.id.team);

        timer = findViewById(R.id.timer);
        try{
            timer.setText(prettyTimer(event.getInt("timer")));
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

            if(event.has("team") && event.getString("team").equals(Main.AWAY_ID)){
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
        if(TabReport.width_team < team.getWidth()) TabReport.width_team = team.getWidth();
    }
    @Override void setFieldWidths(){
        timer.setWidth(TabReport.width_timer);
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
            event.put("team", team.getSelectedItemPosition() == 0 ? Main.HOME_ID : Main.AWAY_ID);

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
    static String prettyTimer(int seconds){
        int minutes = Math.floorDiv(seconds, 60);
        seconds %= 60;
        if(seconds < 10) return minutes + ":0" + seconds;
        return minutes + ":" + seconds;
    }
}
