/*
 * Copyright 2020-2025 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import Toybox.Lang;

class MatchData{
	static const HOME_ID = "home";
	static const AWAY_ID = "away";
	var match_id = 0l;
	const FORMAT = 3;
	var events as Array<Event> = [];
	var home;
	var away;
	var match_type = "15s";
	var period_time = 40;
	var period_count = 2;
	var sinbin = 10;
	var points_try = 5;
	var points_con = 2;
	var points_goal = 3;
	var clock_pk = 60;
	var clock_con = 60;
	var clock_restart = 0;

	function initialize(){
		home = new Team(HOME_ID, HOME_ID, "green");
		away = new Team(AWAY_ID, AWAY_ID, "blue");
	}
	function fromDictionary(match_dict as Dictionary){
		//System.println(match_dict);
		match_id = match_dict.get("match_id") as Long;
		var settings = match_dict.get("settings") as Dictionary;
		match_type = settings.get("match_type");
		period_time = settings.get("period_time");
		period_count = settings.get("period_count");
		sinbin = settings.get("sinbin");
		points_try = settings.get("points_try");
		points_con = settings.get("points_con");
		points_goal = settings.get("points_goal");
		clock_pk = settings.get("clock_pk");
		clock_con = settings.get("clock_con");
		clock_restart = settings.get("clock_restart");
		home.fromDictionary(match_dict.get("home"));
		away.fromDictionary(match_dict.get("away"));
		var events_dict = match_dict.get("events") as Array;
		for(var i=0; i<events_dict.size(); i++){
			var event = new Event(null, null);
			event.fromDictionary(events_dict[i]);
			events.add(event);
		}
	}
	function clear(){
		match_id = 0l;
		events = [];
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
		home.sinbins = [];
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
		away.sinbins = [];
		away.kickoff = false;
	}
	function toDictionary() as Dictionary{
		var events_dict = [];
		for(var i=0; i<events.size(); i++){
			if(events[i].deleted){continue;}
			events_dict.add(events[i].toDictionary());
		}
		return {
			"match_id" => match_id,
			"FORMAT" => FORMAT,
			"settings" => {
				"match_type" => match_type,
				"period_time" => period_time,
				"period_count" => period_count,
				"sinbin" => sinbin,
				"points_try" => points_try,
				"points_con" => points_con,
				"points_goal" => points_goal,
				"clock_pk" => clock_pk,
				"clock_con" => clock_con,
				"clock_restart" => clock_restart
			},
			"home" => home.toDictionary(),
			"away" => away.toDictionary(),
			"events" => events_dict
		};
	}
	function toJson() as String{
		var json = "{\"matchid\":" + match_id;
		json += ",\"format\":" + FORMAT;
		json += ",\"settings\":{";
		json += "\"match_type\":\"" + match_type + "\"";
		json += ",\"period_time\":" + period_time;
		json += ",\"period_count\":" + period_count;
		json += ",\"sinbin\":" + sinbin;
		json += ",\"points_try\":" + points_try;
		json += ",\"points_con\":" + points_con;
		json += ",\"points_goal\":" + points_goal;
		json += ",\"clock_pk\":" + clock_pk;
		json += ",\"clock_con\":" + clock_con;
		json += ",\"clock_restart\":" + clock_restart;
		json += "}";

		json += ",\"home\":" + home.toJson();
		json += ",\"away\":" + away.toJson();

		json += ",\"events\":[";
		for(var i=0; i<events.size(); i++){
			if(events[i].deleted){continue;}
			if(i>0){json += ",";}
			json += events[i].toJson();
		}
		json += "]}";
		return json;
	}
	function alreadyHasYellow(event) as Boolean{
		if(event.who == 0){return false;}
		var count = 0;
		for(var i=0; i<events.size(); i++){
			var e = events[i];
			if(e.what.equals("YELLOW CARD") && e.team.equals(event.team) && e.who == event.who){
				count++;
			}
		}
		return count > 1;
	}
	function convertYellowToRed(event){
		event.what = "RED CARD";
		var team = event.team.equals(HOME_ID) ? MainView.main.match.home : MainView.main.match.away;
		team.yellow_cards--;
		team.red_cards++;
		team.delSinbin(event.id);
	}
	function logEvent(what, team) as Event{
		var event = new Event(what, team);
		events.add(event);
		return event;
	}
	function delEvent(event_id){
		var event = null;
		for(var i=0; i<events.size(); i++){
			if(event_id.toLong() == events[i].id.toLong()){
				event = events[i];
				if(event.what.equals("START")){return false;}
				if(i == events.size()-1){
					if(event.team.equals(HOME_ID)){
						MainView.main.kickClockHomeHide();
					}else{
						MainView.main.kickClockAwayHide();
					}
				}
				break;
			}
		}
		if(event == null){
			System.println("Event not found: " + event_id);
			return true;
		}
		event.deleted = !event.deleted;
		var team = event.team.equals(HOME_ID) ? MainView.main.match.home : MainView.main.match.away;
		switch(event.what){
			case "RED CARD":
				if(event.deleted){
					team.red_cards--;
					if(team.pens > 0){team.pens--;}
				}else{
					team.red_cards++;
					team.pens++;
				}
				break;
			case "YELLOW CARD":
				if(event.deleted){
					team.yellow_cards--;
					if(team.pens > 0){team.pens--;}
					if(team.delSinbin(event.id)){
						MainView.main.updateSinbins();
					}
				}else{
					team.yellow_cards++;
					team.pens++;
				}
				break;
			case "TRY":
				if(event.deleted){
					team.tries--;
				}else{
					team.tries++;
				}
				break;
			case "CONVERSION":
				if(event.deleted){
					team.cons--;
				}else{
					team.cons++;
				}
				break;
			case "PENALTY TRY":
				if(event.deleted){
					team.pen_tries--;
				}else{
					team.pen_tries++;
				}
				break;
			case "PENALTY":
				if(event.deleted){
					team.pens--;
				}else{
					team.pens++;
				}
				break;
			case "DROP GOAL":
				if(event.deleted){
					team.drop_goals--;
				}else{
					team.drop_goals++;
				}
				break;
			case "PENALTY GOAL":
				if(event.deleted){
					team.pen_goals--;
				}else{
					team.pen_goals++;
				}
				break;
		}
		return true;
	}

	class Team{
		var id;
		var team;
		var color;
		var tot = 0;
		var tries = 0;
		var cons = 0;
		var pen_tries = 0;
		var goals = 0;
		var drop_goals = 0;
		var pen_goals = 0;
		var yellow_cards = 0;
		var red_cards = 0;
		var pens = 0;
		var sinbins as Array<Sinbin> = [];
		var kickoff = false;
		function initialize(id, team, color){
			self.id = id;
			self.team = team;
			self.color = color;
		}
		function fromDictionary(team_dict){
			team = team_dict.get("team");
			color = team_dict.get("color");
			tot = team_dict.get("tot");
			tries = team_dict.get("tries");
			cons = team_dict.get("cons");
			pen_tries = team_dict.get("pen_tries");
			goals = team_dict.get("goals");
			drop_goals = team_dict.get("drop_goals");
			pen_goals = team_dict.get("pen_goals");
			yellow_cards = team_dict.get("yellow_cards");
			red_cards = team_dict.get("red_cards");
			pens = team_dict.get("pens");
		}
		function toDictionary() as Dictionary{
			return {
				"id" => id,
				"team" => team,
				"color" => color,
				"tot" => tot,
				"tries" => tries,
				"cons" => cons,
				"pen_tries" => pen_tries,
				"goals" => goals,
				"drop_goals" => drop_goals,
				"pen_goals" => pen_goals,
				"yellow_cards" => yellow_cards,
				"red_cards" => red_cards,
				"pens" => pens
			};
		}
		function toJson() as String{
			var json = "{\"id\":\"" + id + "\"";
			json += ",\"team\":\"" + team + "\"";
			json += ",\"color\":\"" + color + "\"";
			json += ",\"tot\":" + tot;
			json += ",\"tries\":" + tries;
			json += ",\"cons\":" + cons;
			json += ",\"pen_tries\":" + pen_tries;
			json += ",\"yellow_cards\":" + yellow_cards;
			json += ",\"red_cards\":" + red_cards;
			json += ",\"pens\":" + pens;
			json += ",\"kickoff\":" + kickoff + "}";
			return json;
		}
		public function isHome(){return id == MatchData.HOME_ID;}
		function addSinBin(timestamp, end, team) as Sinbin{
			var sinbin = new MatchData.Sinbin(timestamp, end, team);
			sinbins.add(sinbin);
			return sinbin;
		}
		function delSinbin(id) as Boolean{
			for(var i=0; i<sinbins.size(); i++){
				if(sinbins[i].id == id){
					sinbins.remove(sinbins[i]);
					return true;
				}
			}
			return false;
		}
	}
	class Event{
		var id;
		var timer;
		var what;
		var period;
		var team;
		var who = 0;
		var score;
		var deleted = false;
		function initialize(what, team){
			if(what == null){return;}
			id = Utils.getTimestamp();
			self.what = what;
			self.team = team == "" ? null : team;
			timer = MainView.main.getDurationFull();
			period = MainView.main.timer_period;
			if(what.equals("END")){score = MainView.main.match.home.tot + ":" + MainView.main.match.away.tot;}
		}
		function fromDictionary(event_dict){
			id = event_dict.get("id");
			timer = event_dict.get("timer");
			period = event_dict.get("period");
			what = event_dict.get("what");
			team = event_dict.get("team");
			who = event_dict.get("who");
			score = event_dict.get("score");
		}

		function toDictionary() as Dictionary{
			var event_dict = {
				"id" => id,
				"timer" => timer,
				"period" => period,
				"what" => what
			};
			if(team != null){
				event_dict.put("team", team);
				if(who > 0){
					event_dict.put("who", who);
				}
			}
			if(score != null){
				event_dict.put("score", score);
			}
			return event_dict;
		}
		function toJson() as String{
			var json = "{\"id\":" + id + "";
			json += ",\"time\":\"" + Utils.jsonTime(id/1000) + "\"";
			json += ",\"timer\":" + timer;
			json += ",\"period\":" + period;
			json += ",\"what\":\"" + what + "\"";
			if(team != null){json += ",\"team\":\"" + team + "\"";}
			if(who != null && who > 0){json += ",\"who\":" + who;}
			json += "}";
			return json;
		}
	}
	class Sinbin{
		var id;
		var who;
		var team_is_home;
		var end;
		var ended = false;
		var hide = false;
		function initialize(timestamp, end, team_is_home){
			id = timestamp;
			self.end = end;
			self.team_is_home = team_is_home;
		}
	}
}
class CustomMatchType{
	var name = "custom";
	var period_time = 0;
	var period_count = 0;
	var sinbin = 0;
	var points_try = 0;
	var points_con = 0;
	var points_goal = 0;
	var clock_pk = 0;
	var clock_con = 0;
	var clock_restart = 0;
	function initialize(json){
		name = Utils.getJsonString("name", json, name);
		period_time = Utils.getJsonNumber("period_time", json);
		period_count = Utils.getJsonNumber("period_count", json);
		sinbin = Utils.getJsonNumber("sinbin", json);
		points_try = Utils.getJsonNumber("points_try", json);
		points_con = Utils.getJsonNumber("points_con", json);
		points_goal = Utils.getJsonNumber("points_goal", json);
		clock_pk = Utils.getJsonNumber("clock_pk", json);
		clock_con = Utils.getJsonNumber("clock_con", json);
		clock_restart = Utils.getJsonNumber("clock_restart", json);
	}
	function fromDictionary(customMatchType_dict){
		name = customMatchType_dict.get("name");
		period_time = customMatchType_dict.get("period_time");
		period_count = customMatchType_dict.get("period_count");
		sinbin = customMatchType_dict.get("sinbin");
		points_try = customMatchType_dict.get("points_try");
		points_con = customMatchType_dict.get("points_con");
		points_goal = customMatchType_dict.get("points_goal");
		clock_pk = customMatchType_dict.get("clock_pk");
		clock_con = customMatchType_dict.get("clock_con");
		clock_restart = customMatchType_dict.get("clock_restart");
	}
	function toDictionary() as Dictionary{
		return {
			"name" => name,
			"period_time" => period_time,
			"period_count" => period_count,
			"sinbin" => sinbin,
			"points_try" => points_try,
			"points_con" => points_con,
			"points_goal" => points_goal,
			"clock_pk" => clock_pk,
			"clock_con" => clock_con,
			"clock_restart" => clock_restart
		};
	}
}