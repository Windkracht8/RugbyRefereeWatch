/*
 * Copyright 2020-2025 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import Toybox.WatchUi;

class Correct extends Menu2{
	(:typecheck(false))//Check fails on MainView.main.match.events
	function initialize(){
		Menu2.initialize({:title=>Rez.Strings.correct});
		for(var i=MainView.main.match.events.size()-1; i>=0; i--){
			var event = MainView.main.match.events[i];
			if(event.what.equals("TIME OFF") || event.what.equals("RESUME") || event.what.equals("END")){continue;}
			if(event.what.equals("START")){
				var label = Utils.getPeriodName(event.period, MainView.main.match.period_count, event.id) + " " + Utils.jsonTime(event.id/1000);
				addItem(new MenuItem(label, null, event.id, {}));
				continue;
			}
			var label = Utils.prettyTimer(event.timer) + " " + event.what;
			if(event.what.equals("REPLACEMENT")){
				label = Utils.prettyTimer(event.timer) + " " + event.team + " " + Utils.replacementString(event);
			}else if(event.team != null){
				label += " " + event.team;
				if(event.who > 0){
					label += " " + event.who;
				}
			}
			if(event.deleted){
				addItem(new MenuItem(label, Rez.Strings.deleted, event.id, {}));
			}else{
				addItem(new MenuItem(label, null, event.id, {}));
			}
		}
	}
}

class CorrectDelegate extends Menu2InputDelegate{
	function initialize(){Menu2InputDelegate.initialize();}
	function onSelect(item){
		if(MainView.main.match.delEvent(item.getId())){
			popView(SLIDE_UP);
		}
	}
}