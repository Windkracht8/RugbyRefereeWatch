package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MatchData{
    static final String HOME_ID = "home";
    static final String AWAY_ID = "away";
    long match_id;
    private final static int FORMAT = 2;//December 2024; added yellow/red card count
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
    int clock_pk = 60;
    int clock_con = 60;
    int clock_restart = 0;

    MatchData(){
        home = new team(HOME_ID, HOME_ID, "red");
        away = new team(AWAY_ID, AWAY_ID, "blue");
    }
    MatchData(JSONObject match_json) throws JSONException{
        match_id = match_json.getLong("matchid");
        home = new team(match_json.getJSONObject(HOME_ID));
        away = new team(match_json.getJSONObject(AWAY_ID));
        JSONArray events_json = match_json.getJSONArray("events");
        for(int i = 0; i < events_json.length(); i++){
            events.add(new event(events_json.getJSONObject(i)));
        }
    }
    JSONObject toJson(Context context){
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
            settings.put("clock_pk", clock_pk);
            settings.put("clock_con", clock_con);
            settings.put("clock_restart", clock_restart);
            ret.put("settings", settings);
            ret.put(HOME_ID, home.toJson(context));
            ret.put(AWAY_ID, away.toJson(context));
            JSONArray events_json = new JSONArray();
            for(event evt : events){
                events_json.put(evt.toJson(context));
            }
            ret.put("events", events_json);
        }catch(JSONException e){
            Log.e(Main.LOG_TAG, "MatchData.toJson Exception: " + e.getMessage());
            Toast.makeText(context, R.string.fail_read_match, Toast.LENGTH_SHORT).show();
        }
        //Log.d(Main.LOG_TAG, "MatchData.toJson result: " + ret);
        return ret;
    }
    void clear(){
        match_id = 0;
        events.clear();
        home.team = HOME_ID;
        home.tot = 0;
        home.tries = 0;
        home.cons = 0;
        home.pen_tries = 0;
        home.goals = 0;
        home.yellow_cards = 0;
        home.red_cards = 0;
        home.pens = 0;
        home.sinbins.clear();
        home.kickoff = false;
        away.team = AWAY_ID;
        away.tot = 0;
        away.tries = 0;
        away.cons = 0;
        away.pen_tries = 0;
        away.goals = 0;
        away.yellow_cards = 0;
        away.red_cards = 0;
        away.pens = 0;
        away.sinbins.clear();
        away.kickoff = false;
    }
    void removeEvent(event event_del){
        events.remove(event_del);
        team team_edit = event_del.team.equals(HOME_ID) ? home : away;

        switch(event_del.what){
            case "RED CARD":
                team_edit.red_cards--;
                break;
            case "YELLOW CARD":
                team_edit.yellow_cards--;
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
        int yellow_cards = 0;
        int red_cards = 0;
        int pens = 0;
        final ArrayList<sinbin> sinbins = new ArrayList<>();
        boolean kickoff = false;
        private team(String id, String team, String color){
            this.id = id;
            this.team = team;
            this.color = color;
        }
        private team(JSONObject team_js) throws JSONException{
            id = team_js.getString("id");
            team = team_js.getString("team");
            color = team_js.getString("color");
            tot = team_js.getInt("tot");
            tries = team_js.getInt("tries");
            cons = team_js.getInt("cons");
            pen_tries = team_js.getInt("pen_tries");
            goals = team_js.getInt("goals");
            if(team_js.has("yellow_cards")) yellow_cards = team_js.getInt("yellow_cards");
            if(team_js.has("yellow_cards")) red_cards = team_js.getInt("red_cards");
            pens = team_js.getInt("pens");
            kickoff = team_js.getBoolean("kickoff");
        }
        boolean isHome(){return id.equals(HOME_ID);}
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
        private JSONObject toJson(Context context){
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
                ret.put("yellow_cards", yellow_cards);
                ret.put("red_cards", red_cards);
                ret.put("pens", pens);
                ret.put("kickoff", kickoff);
            }catch(JSONException e){
                Log.e(Main.LOG_TAG, "MatchData.match.toJson Exception: " + e.getMessage());
                Toast.makeText(context, R.string.fail_read_match, Toast.LENGTH_SHORT).show();
            }
            return ret;
        }
    }
    static class event{
        private final long id;
        private final String time;
        long timer;
        String what;
        int period;
        String team;
        int who;
        String score;
        private event(String what, long id, String team, int who){
            this.id = id > 0 ? id : System.currentTimeMillis();
            time = Main.prettyTime(this.id);
            timer = (Main.timer_timer + ((long)(Main.timer_period-1)* Main.match.period_time*60000));
            period = Main.timer_period;
            this.what = what;
            this.team = team;
            this.who = who;
            if(what.equals("END")) score = Main.match.home.tot + ":" + Main.match.away.tot;
        }
        private event(JSONObject event_json) throws JSONException{
            id = event_json.getLong("id");
            time = event_json.getString("time");
            timer = event_json.getLong("timer");
            what = event_json.getString("what");
            period = event_json.getInt("period");
            if(event_json.has("team")) team = event_json.getString("team");
            if(event_json.has("who")) who = event_json.getInt("who");
            if(event_json.has("score")) score = event_json.getString("score");
        }
        private JSONObject toJson(Context context){
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
                Toast.makeText(context, R.string.fail_read_match, Toast.LENGTH_SHORT).show();
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
            team_is_home = team.equals(HOME_ID);
            this.who = who;
        }
    }
}
