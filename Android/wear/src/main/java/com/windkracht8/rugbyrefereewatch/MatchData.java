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
        match_id = 0;
        events.clear();
        home.team = "home";
        home.tot = 0;
        home.tries = 0;
        home.cons = 0;
        home.pen_tries = 0;
        home.goals = 0;
        home.pens = 0;
        home.sinbins.clear();
        away.team = "away";
        away.tot = 0;
        away.tries = 0;
        away.cons = 0;
        away.pen_tries = 0;
        away.goals = 0;
        away.pens = 0;
        away.sinbins.clear();
    }
    public void removeEvent(event event_del){
        events.remove(event_del);
        team team_edit = event_del.team.equals("home") ? home : away;

        switch(event_del.what){
            case "YELLOW CARD":
                for(sinbin sb : team_edit.sinbins){
                    if(event_del.id == sb.id){
                        team_edit.sinbins.remove(sb);
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
            case "PENALTY TRY":
                team_edit.pen_tries--;
                break;
            case "PENALTY":
                team_edit.pens--;
                break;
            case "GOAL":
                team_edit.goals--;
                break;
        }
    }
    public void logEvent(String what, String team, Integer who, long id){
        event evt = new event(what, id);
        if(team != null){
            evt.team = team;
            if(who != null && who > 0){
                evt.who = who;
            }
        }
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
        public int pen_tries = 0;
        public int goals = 0;
        public int pens = 0;
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
        public boolean hasSinbin(long id){
            for(sinbin sb : sinbins){
                if(id == sb.id){
                    return true;
                }
            }
            return false;
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
                ret.put("pen_tries", pen_tries);
                ret.put("goals", goals);
                ret.put("pens", pens);
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
        public event(String what, long id){
            this.id = id > 0 ? id : MainActivity.getCurrentTimestamp();
            this.time = MainActivity.prettyTime(id);
            long current_timer = (MainActivity.timer_timer + ((long)(MainActivity.timer_period-1)*MainActivity.match.period_time*60000));
            this.timer = MainActivity.prettyTimer(current_timer);
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
