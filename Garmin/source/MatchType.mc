/*
 * Copyright 2020-2025 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import Toybox.WatchUi;

class MatchType extends Menu2{
	function initialize(){
		Menu2.initialize({:title=>Rez.Strings.details});
		addItem(new MenuItem(Rez.Strings.custom, null, "custom", {}));
		addItem(new MenuItem(Rez.Strings._15s, null, "_15s", {}));
		addItem(new MenuItem(Rez.Strings._10s, null, "_10s", {}));
		addItem(new MenuItem(Rez.Strings._7s, null, "_7s", {}));
		addItem(new MenuItem(Rez.Strings.beach_7s, null, "beach_7s", {}));
		addItem(new MenuItem(Rez.Strings.beach_5s, null, "beach_5s", {}));

		for(var i=0; i<FileStore.customMatchTypes.size(); i++){
			var name = FileStore.customMatchTypes[i];
			addItem(new MenuItem(name, null, name, {}));
		}
	}
}

class MatchTypeDelegate extends Menu2InputDelegate{
	function initialize(){Menu2InputDelegate.initialize();}
	function onSelect(item){
		System.println(item.getId());
		var match = MainView.main.match;
		switch(item.getId()){
			case "custom":
				switchToView(new MatchTypeDetails(), new MatchTypeDetailsDelegate(), SLIDE_UP);
				break;
			case "_15s":
				match.match_type = "15s";
				match.period_time = 40;
				match.period_count = 2;
				match.sinbin = 10;
				match.points_try = 5;
				match.points_con = 2;
				match.points_goal = 3;
				match.clock_pk = 60;
				match.clock_con = 90;
				match.clock_restart = 0;
				break;
			case "_10s":
				match.match_type = "10s";
				match.period_time = 10;
				match.period_count = 2;
				match.sinbin = 2;
				match.points_try = 5;
				match.points_con = 2;
				match.points_goal = 3;
				match.clock_pk = 30;
				match.clock_con = 30;
				match.clock_restart = 0;
				break;
			case "_7s":
				match.match_type = "7s";
				match.period_time = 7;
				match.period_count = 2;
				match.sinbin = 2;
				match.points_try = 5;
				match.points_con = 2;
				match.points_goal = 3;
				match.clock_pk = 30;
				match.clock_con = 30;
				match.clock_restart = 30;
				break;
			case "beach_7s":
				match.match_type = "beach 7s";
				match.period_time = 7;
				match.period_count = 2;
				match.sinbin = 2;
				match.points_try = 1;
				match.points_con = 0;
				match.points_goal = 0;
				match.clock_pk = 15;
				match.clock_con = 0;
				match.clock_restart = 0;
				break;
			case "beach_5s":
				match.match_type = "beach 5s";
				match.period_time = 5;
				match.period_count = 2;
				match.sinbin = 2;
				match.points_try = 1;
				match.points_con = 0;
				match.points_goal = 0;
				match.clock_pk = 15;
				match.clock_con = 0;
				match.clock_restart = 0;
				break;
			default://custom
				var customMatchType = FileStore.readCustomMatchType(item.getId().toString());
				match.match_type = customMatchType.get("name");
				match.period_time = customMatchType.get("period_time");
				match.period_count = customMatchType.get("period_count");
				match.sinbin = customMatchType.get("sinbin");
				match.points_try = customMatchType.get("points_try");
				match.points_con = customMatchType.get("points_con");
				match.points_goal = customMatchType.get("points_goal");
				match.clock_pk = customMatchType.get("clock_pk");
				match.clock_con = customMatchType.get("clock_con");
				match.clock_restart = customMatchType.get("clock_restart");
		}
		MainView.main.timer_period_time = match.period_time*60;
		popView(SLIDE_UP);
	}
}