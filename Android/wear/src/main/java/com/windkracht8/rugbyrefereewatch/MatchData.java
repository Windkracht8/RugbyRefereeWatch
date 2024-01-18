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
    public final int format = 1;//September 2023
    public final ArrayList<event> events = new ArrayList<>();
    public team home;
    public team away;
    public String match_type = "15s";
    public int period_time = 40;
    public int period_count = 2;
    public int sinbin = 10;
    public int points_try = 5;
    public int points_con = 2;
    public int points_goal = 3;

    public MatchData(){
        home = new team("home", "home", "red");
        away = new team("away", "away", "blue");
    }
    public MatchData(Context context, JSONObject match_json){
        try{
            match_id = match_json.getLong("matchid");
            home = new team(context, match_json.getJSONObject("home"));
            away = new team(context, match_json.getJSONObject("away"));
            JSONArray events_json = match_json.getJSONArray("events");
            for(int i = 0; i < events_json.length(); i++){
                events.add(new event(context, events_json.getJSONObject(i)));
            }
        }catch(JSONException e){
            Log.e(Main.RRW_LOG_TAG, "MatchData.MatchData Exception: " + e.getMessage());
            Toast.makeText(context, R.string.fail_read_match, Toast.LENGTH_SHORT).show();
            match_id = 0;
        }
    }
    public JSONObject toJson(Context context){
        JSONObject ret = new JSONObject();
        try{
            ret.put("matchid", match_id);
            ret.put("format", format);
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
            Log.e(Main.RRW_LOG_TAG, "MatchData.toJson Exception: " + e.getMessage());
            Toast.makeText(context, R.string.fail_read_match, Toast.LENGTH_SHORT).show();
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
        event evt = new event(what, id, team, who);
        events.add(evt);
    }
    public static class team{
        public String id;
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
        public team(Context context, JSONObject team_js){
            try{
                id = team_js.getString("id");
                team = team_js.getString("team");
                color = team_js.getString("color");
                tot = team_js.getInt("tot");
                tries = team_js.getInt("tries");
                cons = team_js.getInt("cons");
                pen_tries = team_js.getInt("pen_tries");
                goals = team_js.getInt("goals");
                pens = team_js.getInt("pens");
                kickoff = team_js.getBoolean("kickoff");
            }catch(JSONException e){
                Log.e(Main.RRW_LOG_TAG, "MatchData.team Exception: " + e.getMessage());
                Toast.makeText(context, R.string.fail_read_match, Toast.LENGTH_SHORT).show();
            }
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
                Log.e(Main.RRW_LOG_TAG, "MatchData.match.toJson Exception: " + e.getMessage());
                Toast.makeText(context, R.string.fail_read_match, Toast.LENGTH_SHORT).show();
            }
            return ret;
        }
    }
    public static class event{
        public long id;
        public String time;
        public long timer;
        public String what;
        public int period;
        public String team;
        public int who;
        public String score;
        public event(String what, long id, String team, int who){
            this.id = id > 0 ? id : Main.getCurrentTimestamp();
            this.time = Main.prettyTime(this.id);
            this.timer = (Main.timer_timer + ((long)(Main.timer_period-1)* Main.match.period_time*60000));
            this.period = Main.timer_period;
            this.what = what;
            this.team = team;
            this.who = who;
            if(what.equals("END"))
                this.score = Main.match.home.tot + ":" + Main.match.away.tot;
        }
        public event(Context context, JSONObject event_json){
            try{
                id = event_json.getLong("id");
                time = event_json.getString("time");
                timer = event_json.getLong("timer");
                what = event_json.getString("what");
                period = event_json.getInt("period");
                if(event_json.has("team")) team = event_json.getString("team");
                if(event_json.has("who")) who = event_json.getInt("who");
                if(event_json.has("score")) score = event_json.getString("score");
            }catch(JSONException e){
                Log.e(Main.RRW_LOG_TAG, "MatchData.event Exception: " + e.getMessage());
                Toast.makeText(context, R.string.fail_read_match, Toast.LENGTH_SHORT).show();
            }
        }
        public JSONObject toJson(Context context){
            JSONObject evt = new JSONObject();
            try{
                evt.put("id", id);
                evt.put("time", time);
                evt.put("timer", timer);
                evt.put("period", period);
                evt.put("what", what);
                if(team != null){
                    evt.put("team", team);
                    if(who != 0){
                        evt.put("who", who);
                    }
                }
                if(score != null) {
                    evt.put("score", score);
                }
            }catch(JSONException e){
                Log.e(Main.RRW_LOG_TAG, "MatchData.event.toJson Exception: " + e.getMessage());
                Toast.makeText(context, R.string.fail_read_match, Toast.LENGTH_SHORT).show();
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
