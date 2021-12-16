package com.windkracht8.rugbyrefereewatch.wearos;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class matchdata{
    public long matchid;
    public ArrayList<event> events = new ArrayList<>();
    public team home;
    public team away;
    public String match_type = "15s";
    public int period_time = 40;
    public int period_count = 2;
    public int sinbin = 10;
    public int points_try = 5;
    public int points_con = 2;
    public int points_goal = 3;

    public matchdata(){
        home = new team("home", "home", "green");
        away = new team("away", "away", "red");
    }
    public matchdata(JSONObject match_json){
        home = new team("home", "home", "green");
        away = new team("away", "away", "red");
        try {
            match_type = match_json.getString("match_type");
            period_time = match_json.getInt("period_time");
            period_count = match_json.getInt("period_count");
            sinbin = match_json.getInt("sinbin");
            points_try = match_json.getInt("points_try");
            points_con = match_json.getInt("points_con");
            points_goal = match_json.getInt("points_goal");
        }catch(JSONException e){
            Log.e("matchdata", "matchdata from json: " + e.getMessage());
        }
    }
    public JSONObject tojson(){
        JSONObject ret = new JSONObject();
        try {
            ret.put("matchid", matchid);
            ret.put("match_type", match_type);
            ret.put("period_time", period_time);
            ret.put("period_count", period_count);
            ret.put("sinbin", sinbin);
            ret.put("points_try", points_try);
            ret.put("points_con", points_con);
            ret.put("points_goal", points_goal);
            ret.put("home", home.tojson());
            ret.put("away", away.tojson());
            JSONArray events_json = new JSONArray();
            for(event evt : events){
                events_json.put(evt.tojson());
            }
            ret.put("events", events_json);
        } catch (JSONException e) {
            Log.e("matchdata", "tojson: " + e.getMessage());
        }
        return ret;
    }
    public void clear(){
        for(sinbin sinbin_data : home.sinbins) {
            sinbin_data.hide = true;
        }
        for(sinbin sinbin_data : away.sinbins) {
            sinbin_data.hide = true;
        }
    }
    public void removeEvent(event event_del){
        events.remove(event_del);
        team team_edit = event_del.team.equals("home") ? home : away;

        switch(event_del.what){
            case "YELLOW CARD":
                for(sinbin sinbin : team_edit.sinbins) {
                    if (event_del.id == sinbin.id){
                        sinbin.hide = true;
                        return;
                    }
                }
                break;
            case "TRY":
                team_edit.trys--;
                break;
            case "CONVERSION":
                team_edit.cons--;
                break;
            case "GOAL":
                team_edit.goals--;
                break;
        }
    }
    public void log_event(String what){
        event evt = new event(what);
        events.add(evt);
        Log.i("matchdata" , "log_event: " + evt.tojson());
    }
    public void log_event(String what, String team){
        if(team == null){
            log_event(what);
            return;
        }
        event evt = new event(what);
        evt.team = team;
        events.add(evt);
        Log.i("matchdata" , "log_event: " + evt.tojson());
    }
    public void log_event(String what, String team, int who){
        event evt = new event(what);
        evt.team = team;
        evt.who = who;
        events.add(evt);
        Log.i("matchdata" , "log_event: " + evt.tojson());
    }
    public void log_event(long time, String what, String team, int who){
        event evt = new event(time, what);
        evt.team = team;
        evt.who = who;
        events.add(evt);
        Log.i("matchdata" , "log_event: " + evt.tojson());
    }
    public static class team{
        public String id;
        public String team;
        public String color;
        public int tot = 0;
        public int trys = 0;
        public int cons = 0;
        public int goals = 0;
        public ArrayList<sinbin> sinbins = new ArrayList<>();
        public boolean kickoff = false;
        public team(String id, String team, String color){
            this.id = id;
            this.team = team;
            this.color = color;
        }
        public void add_sinbin(long id, long end){
            sinbin sb = new sinbin(id, end);
            sinbins.add(sb);
        }
        public JSONObject tojson(){
            JSONObject ret = new JSONObject();
            try {
                ret.put("id", id);
                ret.put("team", team);
                ret.put("color", color);
                ret.put("tot", tot);
                ret.put("trys", trys);
                ret.put("cons", cons);
                ret.put("goals", goals);
                ret.put("kickoff", kickoff);
            } catch (JSONException e) {
                Log.e("matchdata", "match.tojson: " + e.getMessage());
            }
            return ret;
        }
    }
    public static class event{
        public long id;
        public String time;
        public String timer;
        public String what;
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
        public JSONObject tojson(){
            JSONObject evt = new JSONObject();
            try {
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
            } catch (JSONException e) {
                Log.e("matchdata" , "event.tojson: " + e.getMessage());
            }
            return evt;
        }
    }
    public static class sinbin{
        public long id;
        public long end;
        public boolean ended = false;
        public boolean hide = false;
        public sinbin(long id, long end){
            this.id = id;
            this.end = end;
        }
    }
}
