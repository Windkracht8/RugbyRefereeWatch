package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MatchData{
    public long match_id;
    public final ArrayList<event> events = new ArrayList<>();
    public final team home;
    public final team away;
    public String match_type = "15s";
    public int period_time = 40;
    public int period_count = 2;
    public int sinbin = 10;
    public int points_try = 5;
    public int points_con = 2;
    public int points_goal = 3;

    public MatchData(){
        home = new team("home", "home", "green");
        away = new team("away", "away", "red");
    }
    public MatchData(Context context, JSONObject match_json){
        home = new team("home", "home", "green");
        away = new team("away", "away", "red");
        try{
            match_id = match_json.getLong("matchid");
            JSONObject settings = match_json.getJSONObject("settings");
            match_type = settings.getString("match_type");
            period_time = settings.getInt("period_time");
            period_count = settings.getInt("period_count");
            sinbin = settings.getInt("sinbin");
            points_try = settings.getInt("points_try");
            points_con = settings.getInt("points_con");
            points_goal = settings.getInt("points_goal");
        }catch(JSONException e){
            Log.e("MatchData", "MatchData from json: " + e.getMessage());
            Toast.makeText(context, "Failed to read match", Toast.LENGTH_SHORT).show();
        }
    }
    public JSONObject toJson(Context context){
        JSONObject ret = new JSONObject();
        try{
            ret.put("matchid", match_id);
            JSONObject settings = new JSONObject();
            settings.put("match_type", match_type);
            settings.put("period_time", period_time);
            settings.put("period_count", period_count);
            settings.put("sinbin", sinbin);
            settings.put("points_try", points_try);
            settings.put("points_con", points_con);
            settings.put("points_goal", points_goal);
            ret.put("settings", settings);
            ret.put("home", home.toJson(context));
            ret.put("away", away.toJson(context));
            JSONArray events_json = new JSONArray();
            for(event evt : events){
                events_json.put(evt.toJson(context));
            }
            ret.put("events", events_json);
        }catch(JSONException e){
            Log.e("MatchData", "toJson: " + e.getMessage());
            Toast.makeText(context, "Failed to read match", Toast.LENGTH_SHORT).show();
        }
        return ret;
    }
    public void clear(){
        for(sinbin sinbin_data : home.sinbins){
            sinbin_data.hide = true;
        }
        for(sinbin sinbin_data : away.sinbins){
            sinbin_data.hide = true;
        }
    }
    public void removeEvent(event event_del){
        events.remove(event_del);
        team team_edit = event_del.team.equals("home") ? home : away;

        switch(event_del.what){
            case "YELLOW CARD":
                for(sinbin sinbin : team_edit.sinbins){
                    if(event_del.id == sinbin.id){
                        sinbin.hide = true;
                        return;
                    }
                }
                break;
            case "TRY":
                team_edit.tries--;
                break;
            case "CONVERSION":
                team_edit.cons--;
                break;
            case "GOAL":
                team_edit.goals--;
                break;
        }
    }
    public void logEvent(String what){
        event evt = new event(what);
        events.add(evt);
        Log.i("MatchData" , "log_event: " + what);
    }
    public void logEvent(String what, String team){
        if(team == null){
            logEvent(what);
            return;
        }
        event evt = new event(what);
        evt.team = team;
        events.add(evt);
        Log.i("MatchData" , "log_event: " + what + " " + team);
    }
    public void logEvent(String what, String team, int who){
        event evt = new event(what);
        evt.team = team;
        evt.who = who;
        events.add(evt);
        Log.i("MatchData" , "log_event: " + what + " " + team + " " + who);
    }
    public void logEvent(long time, String what, String team, int who){
        event evt = new event(time, what);
        evt.team = team;
        evt.who = who;
        events.add(evt);
        Log.i("MatchData" , "log_event: " + what + " " + team + " " + who);
    }
    public static class team{
        public final String id;
        public String team;
        public String color;
        public int tot = 0;
        public int tries = 0;
        public int cons = 0;
        public int goals = 0;
        public final ArrayList<sinbin> sinbins = new ArrayList<>();
        public boolean kickoff = false;
        public team(String id, String team, String color){
            this.id = id;
            this.team = team;
            this.color = color;
        }
        public void addSinbin(long id, long end){
            sinbin sb = new sinbin(id, end);
            sinbins.add(sb);
        }
        public JSONObject toJson(Context context){
            JSONObject ret = new JSONObject();
            try{
                ret.put("id", id);
                ret.put("team", team);
                ret.put("color", color);
                ret.put("tot", tot);
                ret.put("tries", tries);
                ret.put("cons", cons);
                ret.put("goals", goals);
                ret.put("kickoff", kickoff);
            }catch(JSONException e){
                Log.e("MatchData", "match.toJson: " + e.getMessage());
                Toast.makeText(context, "Failed to read match", Toast.LENGTH_SHORT).show();
            }
            return ret;
        }
    }
    public static class event{
        public final long id;
        public final String time;
        public final String timer;
        public final String what;
        public String team = null;
        public int who = 0;
        public event(long id, String what){
            this.id = id;
            this.time = MainActivity.prettyTime(id);
            this.timer = MainActivity.prettyTimer(MainActivity.timer_timer);
            this.what = what;
        }
        public event(String what){
            this.id = MainActivity.getCurrentTimestamp();
            this.time = MainActivity.prettyTime(id);
            this.timer = MainActivity.prettyTimer(MainActivity.timer_timer);
            this.what = what;
        }
        public JSONObject toJson(Context context){
            JSONObject evt = new JSONObject();
            try{
                evt.put("id", id);
                evt.put("time", time);
                evt.put("timer", timer);
                evt.put("what", what);
                if(team != null){
                    evt.put("team", team);
                    if(who != 0){
                        evt.put("who", who);
                    }
                }
            }catch(JSONException e){
                Log.e("MatchData" , "event.toJson: " + e.getMessage());
                Toast.makeText(context, "Failed to read match", Toast.LENGTH_SHORT).show();
            }
            return evt;
        }
    }
    public static class sinbin{
        public final long id;
        public long end;
        public boolean ended = false;
        public boolean hide = false;
        public sinbin(long id, long end){
            this.id = id;
            this.end = end;
        }
    }
}
