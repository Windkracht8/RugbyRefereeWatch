/*
 * Copyright 2020-2025 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import Toybox.Application.Storage;
import Toybox.Graphics;
import Toybox.Lang;
import Toybox.System;
import Toybox.Time;
import Toybox.Timer;
import Toybox.WatchUi;

class MainView extends View{
	static var main;
	var timer_update;
	var timer_time_off_buzz;
	static var height_pixels;
	static var _20vh;
	static var _25vh;
	static var _33vh;
	static var _50vh;
	static var _60vh;
	static var _75vh;
	static var _80vh;
	static var _85vh;
	static var width_pixels;
	static var _15vw;
	static var _20vw;
	static var _40vw;
	static var _50vw;
	static var _60vw;
	static var _80vw;

	var comms;
	var match;

	var v_conf_watch;
	var v_battery;
	var v_time;
	var v_home_bg;
	var v_score_home;
	var v_sinbin_home_1;
	var v_sinbin_home_2;
	var v_sinbin_home_bg;
	var v_away_bg;
	var v_score_away;
	var v_sinbin_away_1;
	var v_sinbin_away_2;
	var v_sinbin_away_bg;
	var v_timer;
	var v_kick_clock_home_label;
	var v_kick_clock_home;
	var v_kick_clock_away_label;
	var v_kick_clock_away;
	var v_pen_home;
	var v_pen;
	var v_pen_away;
	var v_start;
	var v_match_log;
	var v_conf;
	var v_resume;
	var v_end;
	var v_next;
	var v_finish;
	var v_report;
	var v_clear;
	
	var score;
	var score_delegate;
	var foul_play;
	var foul_play_delegate;
	var player_no;
	var conf_watch;
	var conf_watch_delegate;
	var v_delay_end;

	var sinbins_home_count = 0;
	var sinbins_away_count = 0;

	//Timer
	var timer_status = STATUS_CONF;
	var checkpoint_time = 0l;//ms
	var checkpoint_duration = 0;//sec
	var checkpoint_previous = 0;//sec
	var timer_period_ended = false;
	var timer_period = 0;
	var timer_period_time = 2400;//sec
	var timer_type_period = TIMER_TYPE_DOWN;
	var time_off_buzz;

	//Kick clocks
	var kickClockType_home = KICK_CLOCK_TYPE_NONE;
	var kickClockType_away = KICK_CLOCK_TYPE_NONE;	
	var kickClockEnd_home = 0;
	var kickClockEnd_away = 0;

	//Settings
	var screen_on = true;
	var timer_type = TIMER_TYPE_DOWN;
	var record_player = false;
	var record_pens = false;
	var delay_end = true;
	const HELP_VERSION = 1;

	//Buzz
	var VIBE_PROFILE_1;
	var VIBE_PROFILE_3;

	function initialize(){
		View.initialize();
		main = self;
		if(Attention has :vibrate){
			VIBE_PROFILE_1 = [new Attention.VibeProfile(100, 300)];
			VIBE_PROFILE_3 = [
					new Attention.VibeProfile(100, 300),
					new Attention.VibeProfile(0, 500),
					new Attention.VibeProfile(100, 300),
					new Attention.VibeProfile(0, 500),
					new Attention.VibeProfile(100, 300)
			];
		}
		match = new MatchData();
		var settings = Storage.getValue("settings") as Dictionary;
		if(settings != null){
			if(settings.hasKey("home_color")){match.home.color = settings.get("home_color");}
			if(settings.hasKey("away_color")){match.away.color = settings.get("away_color");}
			if(settings.hasKey("away_color")){match.match_type = settings.get("match_type");}
			if(settings.hasKey("period_time")){match.period_time = settings.get("period_time");}
			timer_period_time = match.period_time*60;
			if(settings.hasKey("period_count")){match.period_count = settings.get("period_count");}
			if(settings.hasKey("sinbin")){match.sinbin = settings.get("sinbin");}
			if(settings.hasKey("points_try")){match.points_try = settings.get("points_try");}
			if(settings.hasKey("points_con")){match.points_con = settings.get("points_con");}
			if(settings.hasKey("points_goal")){match.points_goal = settings.get("points_goal");}
			if(settings.hasKey("clock_pk")){match.clock_pk = settings.get("clock_pk");}
			if(settings.hasKey("clock_con")){match.clock_con = settings.get("clock_con");}
			if(settings.hasKey("clock_restart")){match.clock_restart = settings.get("clock_restart");}
			if(settings.hasKey("screen_on")){screen_on = settings.get("screen_on");}
			if(settings.hasKey("timer_type")){timer_type = settings.get("timer_type");}
			if(settings.hasKey("record_player")){record_player = settings.get("record_player");}
			if(settings.hasKey("record_pens")){record_pens = settings.get("record_pens");}
			if(settings.hasKey("delay_end")){delay_end = settings.get("delay_end");}
		}
		Help.showWelcome = settings == null || !settings.hasKey("help_version") || settings.get("help_version") as Number < HELP_VERSION;
	}

	function onLayout(dc as Dc) as Void{
		height_pixels = dc.getHeight();
		_20vh = Math.floor(height_pixels / 5);
		_25vh = Math.floor(height_pixels / 4);
		_33vh = Math.floor(height_pixels / 3);
		_50vh = Math.floor(height_pixels / 2);
		_60vh = Math.floor(height_pixels * .6);
		_75vh = Math.floor(height_pixels * .75);
		_80vh = Math.floor(height_pixels * .8);
		_85vh = Math.floor(height_pixels * .85);
		width_pixels = dc.getWidth();
		_15vw = Math.floor(width_pixels * .15);
		_20vw = Math.floor(width_pixels * .2);
		_40vw = Math.floor(width_pixels * .4);
		_50vw = width_pixels / 2;
		_60vw = Math.floor(width_pixels * .6);
		_80vw = Math.floor(width_pixels * .8);

		Utils.init();
		setLayout(Rez.Layouts.Main(dc));
		v_conf_watch = findDrawableById("conf_watch");
		v_battery = findDrawableById("battery");
		v_time = findDrawableById("time");
		v_home_bg = findDrawableById("home_bg_green");
		v_away_bg = findDrawableById("away_bg_blue");
		v_score_home = findDrawableById("score_home");
		v_sinbin_home_1 = findDrawableById("sinbin_home_1");
		v_sinbin_home_2 = findDrawableById("sinbin_home_2");
		v_sinbin_home_bg = findDrawableById("sinbin_home_bg");
		v_score_away = findDrawableById("score_away");
		v_sinbin_away_1 = findDrawableById("sinbin_away_1");
		v_sinbin_away_2 = findDrawableById("sinbin_away_2");
		v_sinbin_away_bg = findDrawableById("sinbin_away_bg");
		v_timer = findDrawableById("timer");
		v_kick_clock_home_label = findDrawableById("kick_clock_home_label");
		v_kick_clock_home = findDrawableById("kick_clock_home");
		v_kick_clock_away_label = findDrawableById("kick_clock_away_label");
		v_kick_clock_away = findDrawableById("kick_clock_away");
		v_pen_home = findDrawableById("pen_home");
		v_pen = findDrawableById("pen");
		v_pen_away = findDrawableById("pen_away");
		v_start = findDrawableById("start");
		v_match_log = findDrawableById("match_log");
		v_conf = findDrawableById("conf");
		v_resume = findDrawableById("resume");
		v_end = findDrawableById("end");
		v_next = findDrawableById("next");
		v_finish = findDrawableById("finish");
		v_report = findDrawableById("report");
		v_clear = findDrawableById("clear");

		if(highcolor){
			//show the larger icons
			v_conf_watch.setBitmap(Rez.Drawables.ic_conf_watch_high);
			v_match_log.setBitmap(Rez.Drawables.ic_match_log_high);
			v_conf.setBitmap(Rez.Drawables.ic_conf_high);
			//set nicer colors
			v_start.setColor(color_w8blue);
			v_resume.setColor(color_w8blue);
			v_end.setColor(color_w8blue);
			v_next.setColor(color_w8blue);
			v_finish.setColor(color_w8blue);
			v_report.setColor(color_w8blue);
			v_clear.setColor(color_w8blue);
		}

		v_match_log.setLocation(
			v_match_log.locX+(_50vw-v_match_log.width)/2,
			v_match_log.locY+(_25vh-v_match_log.height)/2
		);
		v_conf.setLocation(
			v_conf.locX+(width_pixels-v_conf.width)/2,
			v_conf.locY
		);
		v_conf_watch.setLocation(
			v_conf_watch.locX+(width_pixels-v_conf_watch.width)/2,
			v_conf_watch.locY
		);

		updateBattery();
		var timer_time = new Timer.Timer();
		timer_time.start(method(:updateTime), 1000, true);
		updateTime();
		timer_update = new Timer.Timer();
		timer_time_off_buzz = new Timer.Timer();
		update();
		comms = new Comms();
		comms.start();
		if(Help.showWelcome){
			var help = new Help();
			pushView(help, new HelpDelegate(help), SLIDE_UP);
		}
	}

	function onShow() as Void{
		updateAfterConfig();
		updateScore();
	}
	function updateTime() as Void{
		var time_info = Gregorian.info(Time.now(), Time.FORMAT_LONG);
		var format = time_info.min < 10 ? "$1$:0$2$" : "$1$:$2$";
		format += time_info.sec < 10 ? ":0$3$" : ":$3$";
		v_time.setText(Lang.format(format, [time_info.hour, time_info.min, time_info.sec]));
		if(time_info.sec == 0){updateBattery();}
		requestUpdate();
	}
	function updateBattery(){v_battery.setText(Lang.format("$1$%", [System.getSystemStats().battery.toNumber()]));}
	function update() as Void{
		switch(timer_status){
			case STATUS_RUNNING:
				updateKickClocks();
				updateSinbins();
			case STATUS_REST:
				updateTimer();
				requestUpdate();
				break;
			case STATUS_TIME_OFF:
				if(v_delay_end != null){v_delay_end.progress();}
				break;
		}
		requestUpdate();
		if(timer_status == STATUS_RUNNING || timer_status == STATUS_REST || timer_status == STATUS_TIME_OFF){
			timer_update.start(method(:update), 1000-((Utils.getTimestamp()-checkpoint_time)%1000), false);
		}
	}
	function updateTimer(){
		if(timer_status == STATUS_REST){
			v_timer.setText(Utils.prettyTimer(getDurationPeriod()));
		}else if(timer_type_period == TIMER_TYPE_DOWN){
			v_timer.setText(Utils.prettyTimer(timer_period_time - getDurationPeriod()));
		}else{
			v_timer.setText(Utils.prettyTimer(getDurationFull()));
		}
		if(!timer_period_ended && timer_status == STATUS_RUNNING && getDurationPeriod() > timer_period_time){
			timer_period_ended = true;
			v_timer.setColor(Graphics.COLOR_RED);
			buzz();
		}
	}
	function updateKickClocks(){
		if(kickClockType_home != KICK_CLOCK_TYPE_NONE){
			var left = kickClockEnd_home - getDurationPeriod();
			if(left <= 0){
				kickClockHomeDone();
				return;
			}
			v_kick_clock_home.setText(Utils.prettyTimer(left));
		}
		if(kickClockType_away != KICK_CLOCK_TYPE_NONE){
			var left = kickClockEnd_away - getDurationPeriod();
			if(left <= 0){
				kickClockAwayDone();
				return;
			}
			v_kick_clock_away.setText(Utils.prettyTimer(left));
		}
	}
	function kickClockHomeShow(type as KICK_CLOCK_TYPE){
		kickClockType_home = type;
		kickClockAwayDone();
		switch(type){
			case KICK_CLOCK_TYPE_PK:
				kickClockEnd_home = getDurationPeriod() + match.clock_pk;
				v_kick_clock_home_label.setText(Rez.Strings.pk);
				break;
			case KICK_CLOCK_TYPE_CON:
				kickClockEnd_home = getDurationPeriod() + match.clock_con;
				v_kick_clock_home_label.setText(Rez.Strings.conv);
				break;
			case KICK_CLOCK_TYPE_RESTART:
				kickClockEnd_home = getDurationPeriod() + match.clock_restart;
				v_kick_clock_home_label.setText(Rez.Strings.restart);
				break;
		}
		updateKickClocks();
		v_timer.setSize(_50vw, _50vh);
		v_timer.setFont(Graphics.FONT_NUMBER_MILD);
		v_timer.setLocation(_50vw, _50vh);
		v_kick_clock_home_label.setVisible(true);
		v_kick_clock_home.setVisible(true);
	}
	function kickClockAwayShow(type as KICK_CLOCK_TYPE){
		kickClockType_away = type;
		kickClockHomeDone();
		switch(type){
			case KICK_CLOCK_TYPE_PK:
				kickClockEnd_away = getDurationPeriod() + match.clock_pk;
				v_kick_clock_away_label.setText(Rez.Strings.pk);
				break;
			case KICK_CLOCK_TYPE_CON:
				kickClockEnd_away = getDurationPeriod() + match.clock_con;
				v_kick_clock_away_label.setText(Rez.Strings.conv);
				break;
			case KICK_CLOCK_TYPE_RESTART:
				kickClockEnd_away = getDurationPeriod() + match.clock_restart;
				v_kick_clock_away_label.setText(Rez.Strings.restart);
				break;
		}
		updateKickClocks();
		v_timer.setSize(_50vw, _50vh);
		v_timer.setFont(Graphics.FONT_NUMBER_MILD);
		v_kick_clock_away_label.setVisible(true);
		v_kick_clock_away.setVisible(true);
	}
	function kickClockHomeDone(){
		if(kickClockType_home == KICK_CLOCK_TYPE_NONE){return;}
		if(kickClockType_away == KICK_CLOCK_TYPE_NONE && kickClockType_home == KICK_CLOCK_TYPE_CON && match.clock_restart > 0){
			kickClockHomeShow(KICK_CLOCK_TYPE_RESTART);
			return;
		}
		kickClockHomeHide();
	}
	function kickClockHomeHide(){
		kickClockType_home = KICK_CLOCK_TYPE_NONE;
		kickClockEnd_home = 0;
		v_kick_clock_home_label.setVisible(false);
		v_kick_clock_home.setVisible(false);
		if(kickClockType_away == KICK_CLOCK_TYPE_NONE){
			v_timer.setSize(width_pixels, _50vh);
			v_timer.setFont(Graphics.FONT_NUMBER_HOT);
		}
		v_timer.setLocation(0, _50vh);
		requestUpdate();
	}
	function kickClockAwayDone(){
		if(kickClockType_away == KICK_CLOCK_TYPE_NONE){return;}
		if(kickClockType_home == KICK_CLOCK_TYPE_NONE && kickClockType_away == KICK_CLOCK_TYPE_CON && match.clock_restart > 0){
			kickClockAwayShow(KICK_CLOCK_TYPE_RESTART);
			return;
		}
		kickClockAwayHide();
	}
	function kickClockAwayHide(){
		kickClockType_away = KICK_CLOCK_TYPE_NONE;	
		kickClockEnd_away = 0;
		v_kick_clock_away_label.setVisible(false);
		v_kick_clock_away.setVisible(false);
		if(kickClockType_home == KICK_CLOCK_TYPE_NONE){
			v_timer.setSize(width_pixels, _50vh);
			v_timer.setFont(Graphics.FONT_NUMBER_HOT);
		}
		requestUpdate();
	}
	function updateSinbins(){
		sinbins_home_count = updateSinbinsTeam(
			match.home.sinbins,
			sinbins_home_count,
			v_sinbin_home_1,
			v_sinbin_home_2,
			v_sinbin_home_bg,
			v_score_home
		);
		sinbins_away_count = updateSinbinsTeam(
			match.away.sinbins,
			sinbins_away_count,
			v_sinbin_away_1,
			v_sinbin_away_2,
			v_sinbin_away_bg,
			v_score_away
		);
	}
	(:typecheck(false))//Check fails on sinbins
	function updateSinbinsTeam(sinbins, sinbins_count, v_sinbin_1, v_sinbin_2, v_sinbin_bg, v_score) as Number{
		var sinbins_count_new = 0;
		for(var i=0; i<sinbins.size(); i++){
			var sinbin = sinbins[i];
			if(sinbin.hide){continue;}
			var remaining = sinbin.end - getDurationFull();
			if(remaining < -60){
				sinbin.hide = true;
				continue;
			}

			sinbins_count_new++;
			if(sinbin.ended){continue;}
			if(remaining <= 0){
				remaining = 0;
				sinbin.ended = true;
				buzz();
			}
			if(sinbins_count_new == 1){
				v_sinbin_1.setText(getSinbinText(sinbin, remaining));
			}else{
				v_sinbin_2.setText(getSinbinText(sinbin, remaining));
				break;
			}
		}
		if(sinbins_count > sinbins_count_new){
			//remove a sinbin
			if(sinbins_count == 2){
				v_sinbin_2.setVisible(false);
				v_sinbin_bg.setVisible(true);
			}
			if(sinbins_count_new == 0){
				v_sinbin_1.setVisible(false);
				v_score.setFont(Graphics.FONT_LARGE);
				v_score.setLocation(v_score.locX, _25vh);
			}
		}else if(sinbins_count < sinbins_count_new){
			//add a sinbin
			if(sinbins_count < 1){
				v_sinbin_1.setVisible(true);
				v_score.setFont(Graphics.FONT_MEDIUM);
				v_score.setLocation(v_score.locX, _20vh);
			}
			if(sinbins_count_new == 2){
				v_sinbin_2.setVisible(true);
				v_sinbin_bg.setVisible(false);
			}
		}
		return sinbins_count_new;
	}
	function getSinbinText(sinbin, remaining) as Lang.String{
		var tmp = Utils.prettyTimer(remaining);
		if(sinbin.who != null && sinbin.who > 0){
			if(sinbin.team_is_home){
				tmp = "(" + sinbin.who + ") " + tmp;
			}else{
				tmp += " (" + sinbin.who + ")";
			}
		}
		return tmp;
	}
	function updateScore(){
		match.home.tot = match.home.tries*match.points_try +
				match.home.cons*match.points_con +
				match.home.pen_tries*(match.points_try + match.points_con) +
				match.home.goals*match.points_goal;
		v_score_home.setText(match.home.tot.toString());

		match.away.tot = match.away.tries*match.points_try +
				match.away.cons*match.points_con +
				match.away.pen_tries*(match.points_try + match.points_con) +
				match.away.goals*match.points_goal;
		v_score_away.setText(match.away.tot.toString());
		if(record_pens){
			var pens_home = match.home.pens + match.home.yellow_cards +
				match.home.red_cards + match.home.pen_tries;
			v_pen_home.setText(pens_home.toString());
			var pens_away = match.away.pens + match.away.yellow_cards +
				match.away.red_cards + match.away.pen_tries;
			v_pen_away.setText(pens_away.toString());
		}
		requestUpdate();
	}
	function updateAfterConfig(){
		v_home_bg.setVisible(false);
		v_home_bg = getTeamBg(true);
		v_home_bg.setVisible(true);
		var home_font_color = Utils.getFontColor(match.home.color);
		v_score_home.setColor(home_font_color);
		v_sinbin_home_1.setColor(home_font_color);
		v_sinbin_home_2.setColor(home_font_color);

		v_away_bg.setVisible(false);
		v_away_bg = getTeamBg(false);
		v_away_bg.setVisible(true);
		var away_font_color = Utils.getFontColor(match.away.color);
		v_score_away.setColor(away_font_color);
		v_sinbin_away_1.setColor(away_font_color);
		v_sinbin_away_2.setColor(away_font_color);
		if(record_pens){
			v_pen_home.setVisible(true);
			v_pen.setVisible(true);
			v_pen_away.setVisible(true);
		}else{
			v_pen_home.setVisible(false);
			v_pen.setVisible(false);
			v_pen_away.setVisible(false);
		}
		updateTimer();

		Storage.setValue("settings",{
			"home_color" => match.home.color,
			"away_color" => match.away.color,
			"match_type" => match.match_type,
			"period_time" => match.period_time,
			"period_count" => match.period_count,
			"sinbin" => match.sinbin,
			"points_try" => match.points_try,
			"points_con" => match.points_con,
			"points_goal" => match.points_goal,
			"clock_pk" => match.clock_pk,
			"clock_con" => match.clock_con,
			"clock_restart" => match.clock_restart,
			"screen_on" => screen_on,
			"timer_type" => timer_type,
			"record_player" => record_player,
			"record_pens" => record_pens,
			"delay_end" => delay_end,
			"help_version" => HELP_VERSION
		});
	}
	(:highcolor)
	function getTeamBg(isHome) as Drawable{
		return findDrawableById(isHome ? "highcolor_home_bg_" + match.home.color : "highcolor_away_bg_" + match.away.color);
	}
	(:lowcolor)
	function getTeamBg(isHome) as Drawable{
		return findDrawableById(isHome ? "home_bg_" + match.home.color : "away_bg_" + match.away.color);
	}
	
	function start(){
		//System.println("MainView.start");
		singleBuzz();
		timer_status = STATUS_RUNNING;
		checkpoint_time = Utils.getTimestamp();
		checkpoint_duration = 0;
		if(match.match_id == 0l){
			match.match_id = checkpoint_time;
			score = new Score();
			score_delegate = new ScoreDelegate();
			foul_play = new FoulPlay();
			foul_play_delegate = new FoulPlayDelegate();
			player_no = new PlayerNo();
			conf_watch = new ConfWatch();
			conf_watch_delegate = new ConfWatchDelegate(conf_watch);
		}
		var kickoffTeam = getKickoffTeam();//capture before increasing timer_period
		timer_period++;
		if(timer_period > match.period_count){timer_type_period = TIMER_TYPE_UP;}
		match.logEvent("START", kickoffTeam);
		updateScore();
		update();
		
		v_conf_watch.setVisible(false);
		fakeButtonBg(false);

		v_start.setVisible(false);
		v_match_log.setVisible(false);
		v_conf.setVisible(false);
		v_next.setVisible(false);
		v_finish.setVisible(false);
		requestUpdate();
		comms.stop();
	}
	function timeOff(){
		//System.println("MainView.timeOff");
		singleBuzz();
		checkpoint_duration = getDurationPeriod();
		checkpoint_time = Utils.getTimestamp();
		timer_time_off_buzz.start(method(:buzzTimeOff), 15000, false);
		timer_status = STATUS_TIME_OFF;
		match.logEvent("TIME OFF", null);
		
		fakeButtonBg(true);
		v_resume.setVisible(true);
		v_end.setVisible(true);
		v_conf_watch.setVisible(true);
		requestUpdate();
	}
	function resume(){
		//System.println("MainView.resume");
		singleBuzz();
		timer_status = STATUS_RUNNING;
		checkpoint_time = Utils.getTimestamp();
		match.logEvent("RESUME", null);
		update();

		fakeButtonBg(false);
		v_resume.setVisible(false);
		v_end.setVisible(false);
		v_conf_watch.setVisible(false);
		requestUpdate();
	}
	function delay_end_start(){
		if(!delay_end){
			endPeriod(Utils.getTimestamp());
			return;
		}
		v_delay_end = new DelayEnd(Utils.getTimestamp());
		pushView(v_delay_end, new DelayEndDelegate(), SLIDE_UP);
	}
	function delay_end_finish(delay_end_start_time){
		v_delay_end = null;
		endPeriod(delay_end_start_time);
	}
	(:typecheck(false))//Check fails on match.events
	function endPeriod(delay_end_start_time){
		//System.println("MainView.endPeriod");
		if(match.events.size()>0 && match.events.slice(-1, null)[0].what.equals("TIME OFF")){
			match.events = match.events.slice(0, -1);
		}
		match.logEvent("END", null);
		
		var correct = timer_period_time - checkpoint_duration + checkpoint_previous;
		for(var i=0; i<match.home.sinbins.size(); i++){
			var sinbin = match.home.sinbins[i];
			if(sinbin.ended){continue;}
			sinbin.end += correct;
		}
		for(var i=0; i<match.away.sinbins.size(); i++){
			var sinbin = match.away.sinbins[i];
			if(sinbin.ended){continue;}
			sinbin.end += correct;
		}

		timer_status = STATUS_REST;
		timer_period_ended = false;
		timer_type_period = TIMER_TYPE_UP;
		checkpoint_duration = 0;
		checkpoint_time = delay_end_start_time;
		checkpoint_previous += timer_period_time;

		var kickoffTeam = getKickoffTeam();
		if(kickoffTeam != null){
			if(kickoffTeam.equals(MatchData.HOME_ID)){
				kickoffTeam = match.home.tot + " KICK";
				v_score_home.setText(kickoffTeam);
			}else{
				kickoffTeam = match.away.tot + " KICK";
				v_score_away.setText(kickoffTeam);
			}
		}
		if(kickClockEnd_home > 0){kickClockHomeDone();}
		if(kickClockEnd_away > 0){kickClockAwayDone();}

		v_next.setText(Utils.getPeriodName(timer_period+1, match.period_count, true));
		v_next.setVisible(true);
		v_finish.setVisible(true);
		v_resume.setVisible(false);
		v_end.setVisible(false);
		kickClockHomeHide();
		kickClockAwayHide();
		v_timer.setText(Utils.prettyTimer(0));
		requestUpdate();
	}
	function nextPeriod(){
		//System.println("MainView.nextPeriod");
		timer_status = STATUS_READY;
		timer_type_period = timer_type;

		v_start.setText(loadResource(Rez.Strings.start) + " " + Utils.getPeriodName(timer_period+1, match.period_count, false));
		v_start.setSize(width_pixels, v_start.height);
		v_start.setVisible(true);
		v_next.setVisible(false);
		v_finish.setVisible(false);
		updateTimer();
		requestUpdate();
	}
	function finish(){
		//System.println("MainView.finish");
		if(checkpoint_time+500 > Utils.getTimestamp()){return;}//sticky fingers
		timer_status = STATUS_FINISHED;
		timer_period_time = match.period_time*60;
		timer_type_period = timer_type;
		updateScore();
		v_report.setVisible(true);
		v_clear.setVisible(true);
		v_conf_watch.setVisible(false);
		v_next.setVisible(false);
		v_finish.setVisible(false);
		requestUpdate();

		FileStore.storeMatch(match);
		comms.start();
	}
	function clear(){
		//System.println("MainView.clear");
		timer_status = STATUS_CONF;
		checkpoint_time = 0;
		checkpoint_duration = 0;
		checkpoint_previous = 0;
		timer_period_ended = false;
		timer_period = 0;
		match.clear();
		updateScore();
		updateAfterConfig();
		updateSinbins();
		v_start.setText(loadResource(Rez.Strings.start));
		v_start.setSize(_50vw, v_start.height);
		v_start.setVisible(true);
		v_match_log.setVisible(true);
		v_conf.setVisible(true);
		v_report.setVisible(false);
		v_clear.setVisible(false);
		requestUpdate();
	}
	function fakeButtonBg(on as Boolean){
		v_timer.setColor(on ? color_disabled : color_text);
		v_kick_clock_home_label.setColor(on ? color_disabled : color_text);
		v_kick_clock_home.setColor(on ? color_disabled : color_text);
		v_kick_clock_away_label.setColor(on ? color_disabled : color_text);
		v_kick_clock_away.setColor(on ? color_disabled : color_text);
		v_pen_home.setColor(on ? color_disabled : color_text);
		v_pen.setColor(on ? color_disabled : color_text);
		v_pen_away.setColor(on ? color_disabled : color_text);
	}

	function kickoffHome(){
		match.home.kickoff = !match.home.kickoff;
		v_score_home.setText(match.home.kickoff ? "0 KICK" : "0");
		match.away.kickoff = false;
		v_score_away.setText("0");
		requestUpdate();
	}
	function kickoffAway(){
		match.away.kickoff = !match.away.kickoff;
		v_score_away.setText(match.away.kickoff ? "0 KICK" : "0");
		match.home.kickoff = false;
		v_score_home.setText("0");
		requestUpdate();
	}
	function tryHome(){
		match.home.tries++;
		updateScore();
		var event = match.logEvent("TRY", MatchData.HOME_ID);
		if(record_player){
			pushView(player_no, new PlayerNoDelegate(player_no, event, null), SLIDE_UP);
		}
		kickClockHomeShow(KICK_CLOCK_TYPE_CON);
	}
	function tryAway(){
		match.away.tries++;
		updateScore();
		var event = match.logEvent("TRY", MatchData.AWAY_ID);
		if(record_player){
			pushView(player_no, new PlayerNoDelegate(player_no, event, null), SLIDE_UP);
		}
		kickClockAwayShow(KICK_CLOCK_TYPE_CON);
	}
	function conHome(){
		match.home.cons++;
		updateScore();
		var event = match.logEvent("CONVERSION", MatchData.HOME_ID);
		if(record_player){
			pushView(player_no, new PlayerNoDelegate(player_no, event, null), SLIDE_UP);
		}
		kickClockHomeDone();
	}
	function conAway(){
		match.away.cons++;
		updateScore();
		var event = match.logEvent("CONVERSION", MatchData.AWAY_ID);
		if(record_player){
			pushView(player_no, new PlayerNoDelegate(player_no, event, null), SLIDE_UP);
		}
		kickClockAwayDone();
	}
	function goalHome(){
		match.home.goals++;
		updateScore();
		var event = match.logEvent("GOAL", MatchData.HOME_ID);
		if(record_player){
			pushView(player_no, new PlayerNoDelegate(player_no, event, null), SLIDE_UP);
		}
		kickClockHomeDone();
	}
	function goalAway(){
		match.away.goals++;
		updateScore();
		var event = match.logEvent("GOAL", MatchData.AWAY_ID);
		if(record_player){
			pushView(player_no, new PlayerNoDelegate(player_no, event, null), SLIDE_UP);
		}
		kickClockAwayDone();
	}
	function yellowHome(){
		match.home.yellow_cards++;
		var event = match.logEvent("YELLOW CARD", MatchData.HOME_ID);
		var sinbin = match.home.addSinBin(event.id, getDurationFull() + (match.sinbin*60), true);
		pushView(player_no, new PlayerNoDelegate(player_no, event, sinbin), SLIDE_UP);
	}
	function yellowAway(){
		match.away.yellow_cards++;
		var event = match.logEvent("YELLOW CARD", MatchData.AWAY_ID);
		var sinbin = match.away.addSinBin(event.id, getDurationFull() + (match.sinbin*60), false);
		pushView(player_no, new PlayerNoDelegate(player_no, event, sinbin), SLIDE_UP);
	}
	function redHome(){
		match.home.red_cards++;
		var event = match.logEvent("RED CARD", MatchData.HOME_ID);
		pushView(player_no, new PlayerNoDelegate(player_no, event, null), SLIDE_UP);
	}
	function redAway(){
		match.away.red_cards++;
		var event = match.logEvent("RED CARD", MatchData.AWAY_ID);
		pushView(player_no, new PlayerNoDelegate(player_no, event, null), SLIDE_UP);
	}
	function penTryHome(){
		match.home.pen_tries++;
		updateScore();
		match.logEvent("PENALTY TRY", MatchData.HOME_ID);
	}
	function penTryAway(){
		match.away.pen_tries++;
		updateScore();
		match.logEvent("PENALTY TRY", MatchData.AWAY_ID);
	}
	function penHome(){
		match.home.pens++;
		updateScore();
		match.logEvent("PENALTY", MatchData.HOME_ID);
		kickClockAwayShow(KICK_CLOCK_TYPE_PK);
	}
	function penAway(){
		match.away.pens++;
		updateScore();
		match.logEvent("PENALTY", MatchData.AWAY_ID);
		kickClockHomeShow(KICK_CLOCK_TYPE_PK);
	}
	function kickClockHomeClick(){
		if(kickClockType_home == KICK_CLOCK_TYPE_PK){
			pushView(new KickClockConfirm(Rez.Strings.kick_clock_pk), new KickClockConfirmDelegate(kickClockType_home, true), SLIDE_UP);
		}else if(kickClockType_home == KICK_CLOCK_TYPE_CON){
			pushView(new KickClockConfirm(Rez.Strings.kick_clock_con), new KickClockConfirmDelegate(kickClockType_home, true), SLIDE_UP);
		}else if(kickClockType_home == KICK_CLOCK_TYPE_RESTART){
			kickClockHomeHide();
		}
	}
	function kickClockAwayClick(){
		if(kickClockType_away == KICK_CLOCK_TYPE_PK){
			pushView(new KickClockConfirm(Rez.Strings.kick_clock_pk), new KickClockConfirmDelegate(kickClockType_away, false), SLIDE_UP);
		}else if(kickClockType_away == KICK_CLOCK_TYPE_CON){
			pushView(new KickClockConfirm(Rez.Strings.kick_clock_con), new KickClockConfirmDelegate(kickClockType_away, false), SLIDE_UP);
		}else if(kickClockType_away == KICK_CLOCK_TYPE_RESTART){
			kickClockAwayHide();
		}
	}
	function kickClockConfirm(kickClockType, isHome){
		if(kickClockType == KICK_CLOCK_TYPE_PK){
			if(isHome){
				goalHome();
			}else{
				goalAway();
			}
		}else if(kickClockType == KICK_CLOCK_TYPE_CON){
			if(isHome){
				conHome();
			}else{
				conAway();
			}
		}
	}
	
	function getDurationPeriod(){
		if(timer_status == STATUS_RUNNING || timer_status == STATUS_REST){
			return Math.floor((Utils.getTimestamp() - checkpoint_time) / 1000) + checkpoint_duration;
		}
		return checkpoint_duration;
	}
	function getDurationFull(){
		if(timer_status == STATUS_RUNNING){
			return getDurationPeriod() + checkpoint_previous;
		}
		return checkpoint_duration + checkpoint_previous;
	}
	function getKickoffTeam(){
		if(match.home.kickoff){
			if(timer_period % 2 == 0){
				return MatchData.HOME_ID;
			}else{
				return MatchData.AWAY_ID;
			}
		}
		if(match.away.kickoff){
			if(timer_period % 2 == 0){
				return MatchData.AWAY_ID;
			}else{
				return MatchData.HOME_ID;
			}
		}
		return null;
	}

	function buzzTimeOff(){
		if(timer_status != STATUS_TIME_OFF){return;}
		buzz();
		timer_time_off_buzz.start(method(:buzzTimeOff), 15000, false);
	}
	function buzz(){if(Attention has :vibrate){Attention.vibrate(VIBE_PROFILE_3);}}
	function singleBuzz(){if(Attention has :vibrate){Attention.vibrate(VIBE_PROFILE_1);}}
}
