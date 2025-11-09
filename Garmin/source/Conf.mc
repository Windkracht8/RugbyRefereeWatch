/*
 * Copyright 2020-2025 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import Toybox.WatchUi;

class Conf extends Menu2{
	var home_color;
	var away_color;
	var match_type;

	function initialize(){
		Menu2.initialize({:title=>Rez.Strings.conf});
		home_color = new MenuItem(Rez.Strings.home_color, null, "home_color", {});
		addItem(home_color);
		away_color = new MenuItem(Rez.Strings.away_color, null, "away_color", {});
		addItem(away_color);
		match_type = new MenuItem(Rez.Strings.match_type, null, "match_type", {});
		addItem(match_type);
		addItem(new MenuItem(Rez.Strings.match_type_details, null, "match_type_details", {}));
		addItem(new MenuItem(Rez.Strings.timer_type, MainView.main.timer_type == TIMER_TYPE_UP ? Rez.Strings.up : Rez.Strings.down, "timer_type", {}));
		addItem(new MenuItem(Rez.Strings.record_player, MainView.main.record_player ? Rez.Strings.on : Rez.Strings.off, "record_player", {}));
		addItem(new MenuItem(Rez.Strings.record_pens, MainView.main.record_pens ? Rez.Strings.on : Rez.Strings.off, "record_pens", {}));
		addItem(new MenuItem(Rez.Strings.delay_end, MainView.main.delay_end ? Rez.Strings.on : Rez.Strings.off, "delay_end", {}));
		if(Toybox has :ActivityRecording){
			addItem(new MenuItem(Rez.Strings.track_activity, MainView.main.track_activity ? Rez.Strings.on : Rez.Strings.off, "track_activity", {}));
		}
		addItem(new MenuItem(Rez.Strings.help, null, "help", {}));
	}
	function onShow(){
		home_color.setSubLabel(MainView.main.match.home.color);
		away_color.setSubLabel(MainView.main.match.away.color);
		match_type.setSubLabel(MainView.main.match.match_type);
	}
}

class ConfDelegate extends Menu2InputDelegate{
	var main as MainView;
	function initialize(){
		Menu2InputDelegate.initialize();
		main = MainView.main;
	}
	function onSelect(item){
		switch(item.getId()){
			case "home_color":
				pushView(new Colors(), new ColorsDelegate(true), SLIDE_UP);
				break;
			case "away_color":
				pushView(new Colors(), new ColorsDelegate(false), SLIDE_UP);
				break;
			case "match_type":
				pushView(new MatchType(), new MatchTypeDelegate(), SLIDE_UP);
				break;
			case "match_type_details":
				pushView(new MatchTypeDetails(), new MatchTypeDetailsDelegate(), SLIDE_UP);
				break;
			case "timer_type":
				main.timer_type = main.timer_type == TIMER_TYPE_UP ? TIMER_TYPE_DOWN : TIMER_TYPE_UP;
				main.timer_type_period = main.timer_type;
				item.setSubLabel(main.timer_type == TIMER_TYPE_UP ? Rez.Strings.up : Rez.Strings.down);
				break;
			case "record_player":
				main.record_player = !main.record_player;
				item.setSubLabel(main.record_player ? Rez.Strings.on : Rez.Strings.off);
				break;
			case "record_pens":
				main.record_pens = !main.record_pens;
				item.setSubLabel(main.record_pens ? Rez.Strings.on : Rez.Strings.off);
				break;
			case "delay_end":
				main.delay_end = !main.delay_end;
				item.setSubLabel(main.delay_end ? Rez.Strings.on : Rez.Strings.off);
				break;
			case "track_activity":
				main.track_activity = !main.track_activity;
				item.setSubLabel(main.track_activity ? Rez.Strings.on : Rez.Strings.off);
				break;
			case "help":
				var help = new Help();
				switchToView(help, new HelpDelegate(help), SLIDE_UP);
				break;
		}
	}
}
