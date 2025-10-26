/*
 * Copyright 2020-2025 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import Toybox.Lang;
import Toybox.Math;
import Toybox.System;
import Toybox.Time;
import Toybox.WatchUi;

enum STATUS{STATUS_CONF, STATUS_RUNNING, STATUS_TIME_OFF, STATUS_REST, STATUS_READY, STATUS_FINISHED}
enum KICK_CLOCK_TYPE{KICK_CLOCK_TYPE_NONE, KICK_CLOCK_TYPE_PK, KICK_CLOCK_TYPE_CON, KICK_CLOCK_TYPE_RESTART}
const TIMER_TYPE_UP = 0;
const TIMER_TYPE_DOWN = 1;
var highcolor = false;
var color_w8blue = 0x55ffff;
var color_bg = 0x000000;//TODO why does lowcolor not default to dark on light?
var color_text = 0xffffff;
var color_disabled = 0xaaaaaa;

class Utils{
	static var boot_time;//ms Long
	static var boot_timer;//ms Number
	(:highcolor)
	static function init(){
		System.println("Utils.init highcolor");
		highcolor = true;
		color_w8blue = 0x2bd2e3;
		color_bg = 0x000000;
		color_text = 0xffffff;
	}
	(:lowcolor)
	static function init(){System.println("Utils.init lowcolor");}
	static function setBootTime(){
		boot_time = Time.now().value() * 1000l;
		boot_timer = System.getTimer();
	}
	static function getTimestamp() as Long{
		var sys_timer = System.getTimer();
		if(sys_timer < boot_timer){setBootTime();}
		return boot_time + sys_timer - boot_timer;
	}
	static function prettyDate(timestamp){
		var time = Gregorian.info(new Time.Moment(timestamp), Time.FORMAT_MEDIUM);
		return Lang.format(
			time.min < 10 ? "$1$ $2$ $3$ $4$:0$5$" : "$1$ $2$ $3$ $4$:$5$",
			[time.day_of_week, time.day, time.month, time.hour, time.min]
		);
	}
	static function jsonTime(timestamp){
		var time = Gregorian.info(new Time.Moment(timestamp), Time.FORMAT_MEDIUM);
		if(time.min < 10 && time.sec < 10){
			return Lang.format("$1$:0$2$:0$3$", [time.hour, time.min, time.sec]);
		}else if(time.min < 10){
			return Lang.format("$1$:0$2$:$3$", [time.hour, time.min, time.sec]);
		}else if(time.sec < 10){
			return Lang.format("$1$:$2$:0$3$", [time.hour, time.min, time.sec]);
		}else{
			return Lang.format("$1$:$2$:$3$", [time.hour, time.min, time.sec]);
		}
	}
	static function prettyTimer(secs){
		var minus = "";
		if(secs < 0){
			secs -= secs * 2;
			minus = "-";
		}
		var minutes = Math.floor(secs/60);
		secs %= 60;
		if(secs < 10){
			return minus + minutes + ":0" + secs;
		}else{
			return minus + minutes + ":" + secs;
		}
	}
	static function getPeriodName(period, period_count, long) as String{
		if(period > period_count){
			var ret = WatchUi.loadResource(long ? Rez.Strings.extra_time : Rez.Strings.extra);
			if(period > period_count+1){
				ret += " " + (period - period_count);
			}
			return ret;
		}else if(long && period_count == 2){
			return WatchUi.loadResource(period == 1 ? Rez.Strings.first_half : Rez.Strings.second_half);
		}else{
			var ret = "";
			switch(period){
				case 1:
					ret = WatchUi.loadResource(Rez.Strings._1st);
					break;
				case 2:
					ret = WatchUi.loadResource(Rez.Strings._2nd);
					break;
				case 3:
					ret = WatchUi.loadResource(Rez.Strings._3rd);
					break;
				case 4:
					ret = WatchUi.loadResource(Rez.Strings._4th);
					break;
				default:
					ret = period;
			}
			return long ? ret + WatchUi.loadResource(Rez.Strings.period) : ret;
		}
	}
	static function replacementString(event) as String{
		var who_leave = event.who_leave != null && event.who_leave > 0 ? event.who_leave : "?";
		var who_enter = event.who_enter != null && event.who_enter > 0 ? event.who_enter : "?";
		return who_enter + " replaced " + who_leave;
	}
	static function getFontColor(teamColor){
		switch(teamColor){
			case "gold":
			case "green":
			case "orange":
			case "pink":
			case "white":
				return 0x000000;
			default:
				return 0xffffff;
		}
	}
	static function getJsonNumber(name, json) as Number{
		var start = json.find(name);
		if(start == null){return 0;}
		var item = json.substring(start, json.length());
		var end = item.find(",");
		return item.substring(item.find(":")+1, end==null?item.length():end).toNumber();
	}
	static function getJsonNumberWithFallback(name, json, fallback) as Number{
		var start = json.find(name);
		if(start == null){return fallback;}
		var item = json.substring(start, json.length());
		var end = item.find(",");
		if(end == null){return fallback;}
		return item.substring(item.find(":")+1, end==null?item.length():end).toNumber();
	}
	static function getJsonLong(name, json) as Long{
		var start = json.find(name);
		if(start == null){return 0l;}
		var item = json.substring(start, json.length());
		var end = item.find("}");
		return item.substring(item.find(":")+1, end==null?item.length():end).toLong();
	}
	static function getJsonBoolean(name, json, fallback) as Boolean{
		var start = json.find(name);
		if(start == null){return fallback;}
		var item = json.substring(start, json.length());
		var end = item.find(",");
		if(end == null){end = item.find("}");}
		if(end == null){return fallback;}
		return item.substring(item.find(":")+1, end).equals("true");
	}
	static function getJsonString(name, json, fallback) as String{
		if(json == null || !(json instanceof String)){return "";}
		var start = json.find(name);
		if(start == null){return fallback;}
		var item = json.substring(start+name.length()+3, json.length());
		return item.substring(0, item.find("\""));
	}
	static function getJsonArray(name, json) as String{
		var start = json.find(name);
		if(start == null){return "";}
		var item = json.substring(start, json.length());
		return item.substring(item.find("[")+1, item.find("]"));
	}
}