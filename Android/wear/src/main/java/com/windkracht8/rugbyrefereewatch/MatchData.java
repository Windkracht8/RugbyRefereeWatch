/*
 * Copyright 2020-2026 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
    private final static int FORMAT = 4; //October 2025; REPLACEMENT added
    final ArrayList<Event> events = new ArrayList<>();
    Team home;
    Team away;
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
        home = new Team(HOME_ID, HOME_ID, "red");
        away = new Team(AWAY_ID, AWAY_ID, "blue");
    }
    MatchData(JSONObject match_json) throws JSONException{
        match_id = match_json.getLong("matchid");
        home = new Team(match_json.getJSONObject(HOME_ID));
        away = new Team(match_json.getJSONObject(AWAY_ID));
        JSONArray events_json = match_json.getJSONArray("events");
        for(int i = 0; i < events_json.length(); i++){
            events.add(new Event(events_json.getJSONObject(i)));
        }
    }
    JSONObject toJson(Context context){
        JSONObject ret = new JSONObject();
        JSONObject settings = new JSONObject();
        try{
            ret.put("matchid", match_id);
            ret.put("format", FORMAT);
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
            for(Event evt : events){
                if(evt.deleted) continue;
                events_json.put(evt.toJson(context));
            }
            ret.put("events", events_json);
        }catch(JSONException e){
            Log.e(Main.LOG_TAG, "MatchData.toJson Exception: " + e.getMessage());
            Toast.makeText(context, R.string.fail_store_match, Toast.LENGTH_SHORT).show();
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
        home.drop_goals = 0;
        home.pen_goals = 0;
        home.yellow_cards = 0;
        home.red_cards = 0;
        home.pens = 0;
        home.sinbins.clear();
        home.kickoff = false;
        home.players.clear();
        away.team = AWAY_ID;
        away.tot = 0;
        away.tries = 0;
        away.cons = 0;
        away.pen_tries = 0;
        away.goals = 0;
        away.drop_goals = 0;
        away.pen_goals = 0;
        away.yellow_cards = 0;
        away.red_cards = 0;
        away.pens = 0;
        away.sinbins.clear();
        away.kickoff = false;
        away.players.clear();
    }
    void removeEvent(Event event){
        event.deleted = true;
        Team team_edit = event.team.equals(HOME_ID) ? home : away;
        switch(event.what){
            case "RED CARD":
                team_edit.red_cards--;
                if(team_edit.pens > 0) team_edit.pens--;
                break;
            case "YELLOW CARD":
                team_edit.yellow_cards--;
                if(team_edit.pens > 0) team_edit.pens--;
                for(Sinbin sb : team_edit.sinbins){
                    if(event.id == sb.id){
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
            case "DROP GOAL":
                team_edit.drop_goals--;
                break;
            case "PENALTY GOAL":
                team_edit.pen_goals--;
                break;
        }
    }
    void undeleteEvent(Event event){
        event.deleted = false;
        Team team_edit = event.team.equals(HOME_ID) ? home : away;
        switch(event.what){
            case "RED CARD":
                team_edit.red_cards++;
                team_edit.pens++;
                break;
            case "YELLOW CARD":
                team_edit.yellow_cards++;
                team_edit.pens++;
                break;
            case "TRY":
                team_edit.tries++;
                break;
            case "CONVERSION":
                team_edit.cons++;
                break;
            case "PENALTY TRY":
                team_edit.pen_tries++;
                break;
            case "PENALTY":
                team_edit.pens++;
                break;
            case "DROP GOAL":
                team_edit.drop_goals++;
                break;
            case "PENALTY GOAL":
                team_edit.pen_goals++;
                break;
        }
    }
    boolean alreadyHasYellow(Event event){
        if(event.who == 0) return false;
        int count = 0;
        for(Event e : events){
            if(e.what.equals("YELLOW CARD") && e.team.equals(event.team) && e.who == event.who){
                count++;
            }
        }
        return count > 1;
    }
    void convertYellowToRed(Event event){
        event.what = "RED CARD";
        Team team = event.team.equals(HOME_ID) ? home : away;
        team.yellow_cards--;
        team.red_cards++;
        for(int i=team.sinbins.size()-1; i>=0; i--){
            if(team.sinbins.get(i).id == event.id){
                team.sinbins.remove(i);
                break;
            }
        }
    }
    Event logEvent(String what, String team, long id){
        Event evt = new Event(what, id, team);
        events.add(evt);
        return evt;
    }
    static class Team{
        String id;
        String team;
        String color;
        int tot = 0;
        int tries = 0;
        int cons = 0;
        int pen_tries = 0;
        int goals = 0;//replaced by drop_goals/pen_goals in oct 2025
        int drop_goals = 0;
        int pen_goals = 0;
        int yellow_cards = 0;
        int red_cards = 0;
        int pens = 0;
        final ArrayList<Sinbin> sinbins = new ArrayList<>();
        boolean kickoff = false;
        final ArrayList<Player> players = new ArrayList<>();
        private Team(String id, String team, String color){
            this.id = id;
            this.team = team;
            this.color = color;
        }
        private Team(JSONObject team_js) throws JSONException{
            id = team_js.getString("id");
            team = team_js.getString("team");
            color = team_js.getString("color");
            tot = team_js.getInt("tot");
            tries = team_js.getInt("tries");
            cons = team_js.getInt("cons");
            pen_tries = team_js.getInt("pen_tries");
            goals = team_js.optInt("goals");
            drop_goals = team_js.optInt("drop_goals");
            pen_goals = team_js.optInt("pen_goals");
            yellow_cards = team_js.optInt("yellow_cards");
            red_cards = team_js.optInt("red_cards");
            pens = team_js.getInt("pens");
            kickoff = team_js.getBoolean("kickoff");
            JSONArray players_json = team_js.getJSONArray("events");
            for(int i = 0; i < players_json.length(); i++){
                players.add(new Player(players_json.getJSONObject(i)));
            }
        }
        boolean isHome(){return id.equals(HOME_ID);}
        Sinbin addSinbin(long timestamp, int end, String team){
            Sinbin sb = new Sinbin(timestamp, end, team);
            sinbins.add(sb);
            return sb;
        }
        boolean hasSinbin(long id){
            for(Sinbin sb : sinbins){
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
                ret.put("drop_goals", drop_goals);
                ret.put("pen_goals", pen_goals);
                ret.put("yellow_cards", yellow_cards);
                ret.put("red_cards", red_cards);
                ret.put("pens", pens);
                ret.put("kickoff", kickoff);
                JSONArray players_json = new JSONArray();
                for(Player player : players) players_json.put(player.toJson(context));
                ret.put("players", players_json);
            }catch(JSONException e){
                Log.e(Main.LOG_TAG, "MatchData.match.toJson Exception: " + e.getMessage());
                Toast.makeText(context, R.string.fail_store_match, Toast.LENGTH_SHORT).show();
            }
            return ret;
        }
    }
    static class Event{
        private final long id;
        final String time;
        int timer;
        String what;
        int period;
        String team;
        int who;
        int who_enter;
        int who_leave;
        String score;
        boolean deleted = false;
        private Event(String what, long timestamp, String team){
            id = timestamp > 0 ? timestamp : System.currentTimeMillis();
            time = Utils.prettyTime(id);
            timer = Main.getDurationFull(id);
            period = Main.timer_period;
            this.what = what;
            this.team = team;
            if(what.equals("END")) score = Main.match.home.tot + ":" + Main.match.away.tot;
        }
        private Event(JSONObject event_json) throws JSONException{
            id = event_json.getLong("id");
            time = event_json.getString("time");
            timer = event_json.getInt("timer");
            what = event_json.getString("what");
            period = event_json.getInt("period");
            team = event_json.optString("team");
            who = event_json.optInt("who");
            who_enter = event_json.optInt("who_enter");
            who_leave = event_json.optInt("who_leave");
            score = event_json.optString("score");
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
                    if(who != 0) evt.put("who", who);
                    if(who_enter != 0) evt.put("who_enter", who_enter);
                    if(who_leave != 0) evt.put("who_leave", who_leave);
                }
                if(score != null) evt.put("score", score);
            }catch(JSONException e){
                Log.e(Main.LOG_TAG, "MatchData.event.toJson Exception: " + e.getMessage());
                Toast.makeText(context, R.string.fail_store_match, Toast.LENGTH_SHORT).show();
            }
            return evt;
        }
    }
    static class Sinbin{
        final long id;
        int who;
        final boolean team_is_home;
        int end;
        boolean ended = false;
        boolean hide = false;
        Sinbin(long timestamp, int end, String team){
            id = timestamp;
            this.end = end;
            team_is_home = team.equals(HOME_ID);
        }
    }
    static class Player{
        private final int number;
        private final String name;
        private final boolean front_row;
        private final boolean captain;
        Player(JSONObject player_js) throws JSONException{
            number = player_js.getInt("number");
            name = player_js.getString("name");
            front_row = player_js.getBoolean("front_row");
            captain = player_js.getBoolean("captain");
        }
        private JSONObject toJson(Context context){
            JSONObject ret = new JSONObject();
            try{
                ret.put("number", number);
                ret.put("name", name);
                ret.put("front_row", front_row);
                ret.put("captain", captain);
            }catch(JSONException e){
                Log.e(Main.LOG_TAG, "MatchData.player.toJson Exception: " + e.getMessage());
                Toast.makeText(context, R.string.fail_store_match, Toast.LENGTH_SHORT).show();
            }
            return ret;
        }
    }
}
