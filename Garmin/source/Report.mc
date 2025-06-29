/*
 * Copyright 2020-2025 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import Toybox.Graphics;
import Toybox.Lang;
import Toybox.WatchUi;

class Report extends View{
	var messages as Array<String> = [];
	var size = 0;
	var index = 0;

	function initialize(match as MatchData){
		View.initialize();
		messages.add(match.home.tries + " tries " + match.away.tries);
		if(match.home.pen_tries > 0 || match.away.pen_tries > 0){
			messages.add(match.home.pen_tries + " pen. tries " + match.away.pen_tries);
		}
		if(match.points_con > 0 && (match.home.cons > 0 || match.away.cons > 0)){
			messages.add(match.home.cons + " conv. " + match.away.cons);
		}
		if(match.points_goal > 0 && (match.home.goals > 0 || match.away.goals > 0)){
			messages.add(match.home.goals + " goals " + match.away.goals);
		}
		messages.add(match.home.tot + " score " + match.away.tot);
		messages.add("");

		if(match.home.yellow_cards > 0 || match.away.yellow_cards > 0){
			messages.add(match.home.yellow_cards + " yellow cards " + match.away.yellow_cards);
		}
		if(match.home.red_cards > 0 || match.away.red_cards > 0){
			messages.add(match.home.red_cards + " red cards " + match.away.red_cards);
		}
		if(match.home.pens > 0 || match.away.pens > 0){
			messages.add(match.home.pens + " penalties " + match.away.pens);
		}
		messages.add("");

		for(var i=0; i<match.events.size(); i++){
			var event = match.events[i] as MatchData.Event;
			if(event.what.equals("RESUME") || event.what.equals("TIME OFF") || event.deleted){continue;}

			var text = Utils.prettyTimer(event.timer) + " ";
			switch(event.what){
				case "START":
					text += "Start " + Utils.getPeriodName(event.period, match.period_count, false);
					break;
				case "END":
					text += "Result " + Utils.getPeriodName(event.period, match.period_count, false) + " " + event.score;
					break;
				case "YELLOW CARD":
					text += "YELLOW";
					break;
				case "RED CARD":
					text += "RED";
					break;
				default:
					text += event.what;
					break;
			}
			if(event.team != null){
				text += " " + event.team;
				if(event.who != null && event.who > 0){text += " " + event.who;}
			}
			messages.add(text);
		}
		size = ((messages.size()-1)/7)+1;
	}
	function onLayout(dc as Dc) as Void{
		setLayout(Rez.Layouts.Report(dc));
		go(0);
	}
	function go(Index){
		index = Index;
		for(var i=1; i<=7; i++){
			(findDrawableById("item_" + i) as TextArea).setText(
				messages.size()>=index*7+i ? messages[index*7+i-1] : ""
			);
		}
		findDrawableById("up").setVisible(index > 0);
		findDrawableById("down").setVisible(size > index+1);
		requestUpdate();
	}
	function goDown(){
		if(index+1 >= size){return;}
		go(index+1);
	}
	function goUp(){
		if(index <= 0){return;}
		go(index-1);
	}
}

class ReportDelegate extends BehaviorDelegate{
	var report;
	function initialize(report){
		BehaviorDelegate.initialize();
		self.report = report;
	}
	function onBack(){
		popView(SLIDE_UP);
		return true;
	}
	function onTap(evt){
		if(evt.getCoordinates()[0] < MainView._15vw){
			onBack();
			return true;
		}
		var y = evt.getCoordinates()[1];
		if(y < MainView._25vh){
			report.goUp();
		}else if(y > MainView._75vh){
			report.goDown();
		}
		return true;
	}
	function onPreviousPage(){
		report.goUp();
		return true;
	}
	function onNextPage(){
		report.goDown();
		return true;
	}
}