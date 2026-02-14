/*
 * Copyright 2020-2026 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import Toybox.Communications;
import Toybox.Lang;
import Toybox.WatchUi;

class Comms{
	var isRegistered = false;
	function start(){
		//System.println("Comms.start");
		Communications.registerForPhoneAppMessages(method(:gotRequest));
		isRegistered = true;
	}
	function stop(){
		//System.println("Comms.stop");
		Communications.registerForPhoneAppMessages(null);
		isRegistered = false;
	}

	function gotRequest(message as Communications.PhoneAppMessage) as Void{
		var request = message.data as String;
		//System.println("Comms.gotRequest: " + request);
		var requestType = Utils.getJsonString("requestType", request, "");
		if(requestType == ""){
			//System.println("Comms.gotRequest no requestType");
			sendResponse("{\"requestType\":\"unknown\",\"responseData\":\"no requestType\"}");
			return;
		}
		if(!(request has :find) || request.find("requestData") == null){
			//System.println("Comms.gotRequest no requestData");
			sendResponse("{\"requestType\":\"" + requestType + "\",\"responseData\":\"no requestData\"}");
			return;
		}
		switch(requestType){
			case "sync":
				onReceiveSync(request);
				break;
			case "getMatch":
				onReceiveGetMatch(request);
				break;
			case "delMatch":
				onReceiveDelMatch(request);
				break;
			case "prepare":
				onReceivePrepare(request);
				break;
			default:
				//System.println("Comms.gotRequest Unknown requestType: " + requestType);
				sendResponse( "{\"requestType\":\"" + requestType + "\",\"responseData\":\"unknown requestType\"}");
		}
	}
	function onReceiveSync(request){
		//{"version":2,"requestType":"sync","requestData":{"custom_match_types":[]}}
		//custom_match_type: {"name":"U18","period_time":30,"period_count":2,"sinbin":8,"points_try":5,"points_con":2,"points_goal":3,"clock_pk":60,"clock_con":60,"clock_restart":0}
		var custom_match_types = Utils.getJsonArray("custom_match_types", request);
		var custom_match_types_array = [];
		while(custom_match_types.length() > 0){
			var start = custom_match_types.find("{");
			var end = custom_match_types.find("}");
			custom_match_types_array.add(new CustomMatchType(custom_match_types.substring(start+1, end == null ? custom_match_types.length() : end)));
			if(end == null){
				break;
			}else{
				custom_match_types = custom_match_types.substring(end+1, custom_match_types.length());
			}
		}
		for(var iw=FileStore.customMatchTypes.size()-1; iw>=0; iw--){
			delCustomMatchTypeIf(FileStore.customMatchTypes[iw], custom_match_types_array);
		}
		for(var i=0; i<custom_match_types_array.size(); i++){
			FileStore.storeCustomMatchType(custom_match_types_array[i]);
		}
		sendResponse(buildSyncResponse());
	}
	function delCustomMatchTypeIf(name, custom_match_types_array as Array<CustomMatchType>){
		for(var ip=0; ip<custom_match_types_array.size(); ip++){
			if(name.hashCode() == custom_match_types_array[ip].name.hashCode()){
				return;
			}
		}
		FileStore.delCustomMatchType(name);
	}
	(:typecheck(false))//Check fails on FileStore.match_ids
	function buildSyncResponse() as String{
		var syncResponse = "{\"requestType\":\"sync\",\"responseData\":{\"match_ids\":[";
		for(var i=0; i<FileStore.match_ids.size(); i++){
			if(i>0){syncResponse += ",";}
			syncResponse += FileStore.match_ids[i];
		}
		syncResponse += "],\"settings\":{";
		syncResponse += "\"home_name\":\"" + MainView.main.match.home.team + "\"";
		syncResponse += ",\"home_color\":\"" + MainView.main.match.home.color + "\"";
		syncResponse += ",\"away_name\":\"" + MainView.main.match.away.team + "\"";
		syncResponse += ",\"away_color\":\"" + MainView.main.match.away.color + "\"";
		syncResponse += ",\"match_type\":\"" + MainView.main.match.match_type + "\"";
		syncResponse += ",\"period_time\":" + MainView.main.match.period_time;
		syncResponse += ",\"period_count\":" + MainView.main.match.period_count;
		syncResponse += ",\"period_time\":" + MainView.main.match.period_time;
		syncResponse += ",\"sinbin\":" + MainView.main.match.sinbin;
		syncResponse += ",\"points_try\":" + MainView.main.match.points_try;
		syncResponse += ",\"points_con\":" + MainView.main.match.points_con;
		syncResponse += ",\"points_goal\":" + MainView.main.match.points_goal;
		syncResponse += ",\"clock_pk\":" + MainView.main.match.clock_pk;
		syncResponse += ",\"clock_con\":" + MainView.main.match.clock_con;
		syncResponse += ",\"clock_restart\":" + MainView.main.match.clock_restart;
		syncResponse += ",\"screen_on\":" + MainView.main.screen_on;
		syncResponse += ",\"timer_type\":" + MainView.main.timer_type;
		syncResponse += ",\"record_player\":" + MainView.main.record_player;
		syncResponse += ",\"record_pens\":" + MainView.main.record_pens;
		syncResponse += ",\"delay_end\":" + MainView.main.delay_end;
		syncResponse += "}}}";
		return syncResponse;
	}
	function onReceiveGetMatch(request){
		//{"requestType":"getMatch","requestData":123456789}
		var match_id = Utils.getJsonLong("requestData", request);
		if(match_id == null || match_id == 0){
			sendResponse("{\"requestType\":\"getMatch\",\"responseData\":\"match not found\"}");
			return;
		}
		sendResponse("{\"requestType\":\"getMatch\",\"responseData\":" + FileStore.readMatch(match_id).toJson() + "}");
	}
	function onReceiveDelMatch(request){
		//{"requestType":"delMatch","requestData":123456789}
		var match_id = Utils.getJsonLong("requestData", request);
		if(match_id == null || match_id == 0){
			sendResponse("{\"requestType\":\"delMatch\",\"responseData\":\"match not found\"}");
			return;
		}
		FileStore.delMatch(match_id);
		sendResponse("{\"requestType\":\"delMatch\",\"responseData\":\"okilly dokilly\"}");
	}
	function onReceivePrepare(request){
		if(MainView.main.timer_status != STATUS_CONF){
			sendResponse("{\"requestType\":\"prepare\",\"responseData\":\"match ongoing\"}");
			return;
		}
		MainView.main.match.home.team = Utils.getJsonString("home_name", request, "home");
		MainView.main.match.home.color = Utils.getJsonString("home_color", request, "green");
		MainView.main.match.away.team = Utils.getJsonString("away_name", request, "away");
		MainView.main.match.away.color = Utils.getJsonString("away_color", request, "blue");
		MainView.main.match.match_type = Utils.getJsonString("match_type", request, "custom");
		MainView.main.match.period_time = Utils.getJsonNumber("period_time", request);
		MainView.main.timer_period_time = MainView.main.match.period_time*60;
		MainView.main.match.period_count = Utils.getJsonNumber("period_count", request);
		MainView.main.match.sinbin = Utils.getJsonNumber("sinbin", request);
		MainView.main.match.points_try = Utils.getJsonNumber("points_try", request);
		MainView.main.match.points_con = Utils.getJsonNumber("points_con", request);
		MainView.main.match.points_goal = Utils.getJsonNumber("points_goal", request);
		MainView.main.match.clock_pk = Utils.getJsonNumber("clock_pk", request);
		MainView.main.match.clock_con = Utils.getJsonNumber("clock_con", request);
		MainView.main.match.clock_restart = Utils.getJsonNumber("clock_restart", request);
		var timer_type = Utils.getJsonNumberWithFallback("timer_type", request, MainView.main.timer_type);
		MainView.main.timer_type = timer_type == TIMER_TYPE_UP ? TIMER_TYPE_UP : TIMER_TYPE_DOWN;
		MainView.main.timer_type_period = MainView.main.timer_type;
		MainView.main.record_player = Utils.getJsonBoolean("record_player", request, MainView.main.record_player);
		MainView.main.record_pens = Utils.getJsonBoolean("record_pens", request, MainView.main.record_pens);
		MainView.main.delay_end = Utils.getJsonBoolean("delay_end", request, MainView.main.delay_end);
		//TODO home_players
		//TODO away_players
		sendResponse("{\"requestType\":\"prepare\",\"responseData\":\"okilly dokilly\"}");
		MainView.main.updateAfterConfig();
	}
	function sendResponse(response as String){
		//System.println("Comms.sendResponse: " + response);
		Communications.transmit(response, null, new Communications.ConnectionListener());
	}
}
