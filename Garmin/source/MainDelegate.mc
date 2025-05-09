/*
 * Copyright 2020-2025 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import Toybox.WatchUi;

class MainDelegate extends BehaviorDelegate{
	var main;
	function initialize(){
		BehaviorDelegate.initialize();
		main = MainView.main;
	}

	function onBack(){
		if(main.timer_status == STATUS_CONF){
			System.exit();
		}else{
			pushView(new Correct(), new CorrectDelegate(), SLIDE_UP);
		}
		return true;
	}
	function onKey(keyEvent){
		if(keyEvent.getKey() == KEY_ENTER){
			if(main.timer_status == STATUS_CONF){
				main.start();
			}else{
				onOverTimerTap();
			}
			return true;
		}
		return false;
	}
	function onTap(evt){
		var y = evt.getCoordinates()[1];
		if(y < MainView._25vh){
			onConfWatchTap();
		}else if(y < MainView._50vh){
			if(evt.getCoordinates()[0] < MainView._50vw){
				onHomeTap();
			}else{
				onAwayTap();
			}
		}else if(y < MainView._75vh){
			if(evt.getCoordinates()[0] < MainView._50vw){
				onOverTimerLeftTap();
			}else{
				onOverTimerRightTap();
			}
		}else if(y < MainView._85vh){
			onBottomTap();
		}else{
			if(evt.getCoordinates()[0] < MainView._50vw){
				onPenHomeTap();
			}else{
				onPenAwayTap();
			}
		}
		return true;
	}

	function onConfWatchTap(){
		switch(main.timer_status){
			case STATUS_TIME_OFF:
			case STATUS_REST:
				pushView(main.conf_watch, main.conf_watch_delegate, SLIDE_UP);
		}
	}
	function onHomeTap(){
		switch(main.timer_status){
			case STATUS_CONF:
				main.kickoffHome();
				break;
			case STATUS_RUNNING:
			case STATUS_TIME_OFF:
			case STATUS_REST:
				main.score_delegate.isHome = true;
				pushView(main.score, main.score_delegate, SLIDE_UP);
		}
	}
	function onAwayTap(){
		switch(main.timer_status){
			case STATUS_CONF:
				main.kickoffAway();
				break;
			case STATUS_RUNNING:
			case STATUS_TIME_OFF:
			case STATUS_REST:
				main.score_delegate.isHome = false;
				pushView(main.score, main.score_delegate, SLIDE_UP);
		}
	}
	function onTimerTap(){
		if(main.timer_status != STATUS_RUNNING){return;}
		main.timeOff();
	}
	function onOverTimerLeftTap(){
		if(main.timer_status == STATUS_CONF){
			main.start();
		}else if(main.kickClockType_home != KICK_CLOCK_TYPE_NONE){
			main.kickClockHomeClick();
		}else{
			onOverTimerTap();
		}
	}
	function onOverTimerRightTap(){
		if(main.timer_status == STATUS_CONF){
			var matchLog = new MatchLog();
			pushView(matchLog, new MatchLogDelegate(matchLog), SLIDE_UP);
		}else if(main.kickClockType_away != KICK_CLOCK_TYPE_NONE){
			main.kickClockAwayClick();
		}else{
			onOverTimerTap();
		}
	}
	function onOverTimerTap(){
		switch(main.timer_status){
			case STATUS_RUNNING:
				onTimerTap();
				break;
			case STATUS_TIME_OFF:
				main.resume();
				break;
			case STATUS_REST:
				main.nextPeriod();
				break;
			case STATUS_READY:
				main.start();
				break;
			case STATUS_FINISHED:
				var report = new Report(main.match);
				pushView(report, new ReportDelegate(report), SLIDE_UP);
		}
	}
	function onBottomTap(){
		switch(main.timer_status){
			case STATUS_CONF:
				pushView(new Conf(), new ConfDelegate(), SLIDE_UP);
				break;
			case STATUS_RUNNING:
				onTimerTap();
				break;
			case STATUS_TIME_OFF:
				main.delay_end_start();
				break;
			case STATUS_REST:
				main.finish();
				break;
			case STATUS_FINISHED:
				main.clear();
		}
	}
	function onPenHomeTap(){
		if(main.timer_status == STATUS_RUNNING){
			main.penHome();
		}else{
			onBottomTap();
		}
	}
	function onPenAwayTap(){
		if(main.timer_status == STATUS_RUNNING){
			main.penAway();
		}else{
			onBottomTap();
		}
	}
}