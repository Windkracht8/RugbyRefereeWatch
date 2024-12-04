package com.windkracht8.rugbyrefereewatch;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

class MatchData{
    long match_id;
    /** @noinspection FieldCanBeLocal*/
    private final int FORMAT = 1;//September 2023
    final ArrayList<event> events = new ArrayList<>();
    team home;
    team away;
    String match_type = "15s";
    int period_time = 40;
    int period_count = 2;
    int sinbin = 10;
    int points_try = 5;
    int points_con = 2;
    int points_goal = 3;

    MatchData(){
        home = new team("home", "home", "red");
        away = new team("away", "away", "blue");
    }
    MatchData(Main  main, JSONObject match_json){
        try{
            match_id = match_json.getLong("matchid");
            home = new team(main, match_json.getJSONObject("home"));
            away = new team(main, match_json.getJSONObject("away"));
            JSONArray events_json = match_json.getJSONArray("events");
            for(int i = 0; i < events_json.length(); i++){
                events.add(new event(main, events_json.getJSONObject(i)));
            }
        }catch(JSONException e){
            Log.e(Main.LOG_TAG, "MatchData.MatchData Exception: " + e.getMessage());
            main.toast(R.string.fail_read_match);
            match_id = 0;
        }
    }
    JSONObject toJson(Main main){
        JSONObject ret = new JSONObject();
        try{
            ret.put("matchid", match_id);
            ret.put("format", FORMAT);
            JSONObject settings = new JSONObject();
            settings.put("match_type", match_type);
            settings.put("period_time", period_time);
            settings.put("period_count", period_count);
            settings.put("sinbin", sinbin);
            settings.put("points_try", points_try);
            settings.put("points_con", points_con);
            settings.put("points_goal", points_goal);
            ret.put("settings", settings);
            ret.put("home", home.toJson(main));
            ret.put("away", away.toJson(main));
            JSONArray events_json = new JSONArray();
            for(event evt : events){
                events_json.put(evt.toJson(main));
            }
            ret.put("events", events_json);
        }catch(JSONException e){
            Log.e(Main.LOG_TAG, "MatchData.toJson Exception: " + e.getMessage());
            main.toast(R.string.fail_read_match);
        }
        return ret;
    }
    void clear(){
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
        home.kickoff = false;
        away.team = "away";
        away.tot = 0;
        away.tries = 0;
        away.cons = 0;
        away.pen_tries = 0;
        away.goals = 0;
        away.pens = 0;
        away.sinbins.clear();
        away.kickoff = false;
    }
    void removeEvent(event event_del){
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
    void logEvent(String what, String team, Integer who, long id){
        event evt = new event(what, id, team, who);
        events.add(evt);
    }
    static class team{
        String id;
        String team;
        String color;
        int tot = 0;
        int tries = 0;
        int cons = 0;
        int pen_tries = 0;
        int goals = 0;
        int pens = 0;
        final ArrayList<sinbin> sinbins = new ArrayList<>();
        boolean kickoff = false;
        private team(String id, String team, String color){
            this.id = id;
            this.team = team;
            this.color = color;
        }
        private team(Main main, JSONObject team_js){
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
                Log.e(Main.LOG_TAG, "MatchData.team Exception: " + e.getMessage());
                main.toast(R.string.fail_read_match);
            }
        }
        void addSinbin(long id, long end, String team, int who){
            sinbin sb = new sinbin(id, end, team, who);
            sinbins.add(sb);
        }
        boolean hasSinbin(long id){
            for(sinbin sb : sinbins){
                if(id == sb.id){
                    return true;
                }
            }
            return false;
        }
        private JSONObject toJson(Main main){
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
                Log.e(Main.LOG_TAG, "MatchData.match.toJson Exception: " + e.getMessage());
                main.toast(R.string.fail_read_match);
            }
            return ret;
        }
    }
    static class event{
        private long id;
        private String time;
        long timer;
        String what;
        int period;
        String team;
        int who;
        String score;
        private event(String what, long id, String team, int who){
            this.id = id > 0 ? id : Main.getCurrentTimestamp();
            time = Main.prettyTime(this.id);
            timer = (Main.timer_timer + ((long)(Main.timer_period-1)* Main.match.period_time*60000));
            period = Main.timer_period;
            this.what = what;
            this.team = team;
            this.who = who;
            if(what.equals("END"))
                score = Main.match.home.tot + ":" + Main.match.away.tot;
        }
        private event(Main main, JSONObject event_json){
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
                Log.e(Main.LOG_TAG, "MatchData.event Exception: " + e.getMessage());
                main.toast(R.string.fail_read_match);
            }
        }
        private JSONObject toJson(Main main){
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
                Log.e(Main.LOG_TAG, "MatchData.event.toJson Exception: " + e.getMessage());
                main.toast(R.string.fail_read_match);
            }
            return evt;
        }
    }
    static class sinbin{
        final long id;
        final int who;
        final boolean team_is_home;
        long end;
        boolean ended = false;
        boolean hide = false;
        sinbin(long id, long end, String team, int who){
            this.id = id;
            this.end = end;
            team_is_home = team.equals("home");
            this.who = who;
        }
    }
}
