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
	static function prettyTime(timestamp){
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
	static function getPeriodName(period, period_count, long) as Lang.String{
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
	static function getJsonString(name, json, fallback) as String{
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