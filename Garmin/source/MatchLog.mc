/*
 * Copyright 2020-2025 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import Toybox.Graphics;
import Toybox.WatchUi;

class MatchLog extends View{
	var v_up;
	var v_down;
	var v_item_1_date;
	var v_item_1_teams;
	var v_item_1_result;
	var v_item_2_date;
	var v_item_2_teams;
	var v_item_2_result;
	var v_match_log_bg_2;
	var match_ids;
	var pages = 0;
	var index = 0;

	function initialize(){
		View.initialize();
		match_ids = FileStore.match_ids.reverse();
		pages = (match_ids.size() + (match_ids.size() % 2)) / 2;
	}
	function onLayout(dc as Dc) as Void{
		setLayout(Rez.Layouts.MatchLog(dc));
		v_up = findDrawableById("up");
		v_down = findDrawableById("down");
		v_item_1_date = findDrawableById("item_1_date");
		v_item_1_teams = findDrawableById("item_1_teams");
		v_item_1_result = findDrawableById("item_1_result");
		v_item_2_date = findDrawableById("item_2_date");
		v_item_2_teams = findDrawableById("item_2_teams");
		v_item_2_result = findDrawableById("item_2_result");
		v_match_log_bg_2 = findDrawableById("match_log_bg_2");
		go(0);
	}
	(:typecheck(false))//Check fails on FileStore.match_ids
	function go(Index){
		index = Index;

		if(match_ids.size()>=(index*2+1)){
			var match = FileStore.readMatch(match_ids[index*2]);
			v_item_1_date.setText(Utils.prettyDate(match.match_id/1000));
			v_item_1_teams.setText(match.home.team + " : " + match.away.team);
			v_item_1_result.setText(match.home.tot + " : " + match.away.tot);
		}else{
			v_item_1_date.setText("");
			v_item_1_teams.setText("empty");
			v_item_1_result.setText("");
		}

		if(match_ids.size()>=(index*2+2)){
			var match = FileStore.readMatch(match_ids[index*2+1]);
			v_item_2_date.setText(Utils.prettyDate(match.match_id/1000));
			v_item_2_teams.setText(match.home.team + " : " + match.away.team);
			v_item_2_result.setText(match.home.tot + " : " + match.away.tot);
			v_match_log_bg_2.setVisible(true);
		}else{
			v_item_2_date.setText("");
			v_item_2_teams.setText("");
			v_item_2_result.setText("");
			v_match_log_bg_2.setVisible(false);
		}

		v_up.setVisible(index > 0);
		v_down.setVisible(pages > index+1);
		requestUpdate();
	}
	function goDown(){
		if(index+1 >= pages){return;}
		go(index+1);
	}
	function goUp(){
		if(index <= 0){return;}
		go(index-1);
	}
	(:typecheck(false))//Check fails on FileStore.match_ids
	function open(item){
		if(match_ids.size()<index*2+item){return;}
		var report = new Report(FileStore.readMatch(match_ids[index*2+item-1]));
		pushView(report, new ReportDelegate(report), SLIDE_UP);
	}
}

class MatchLogDelegate extends BehaviorDelegate{
	var matchLog;
	function initialize(matchLog){
		BehaviorDelegate.initialize();
		self.matchLog = matchLog;
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
		if(y < MainView._20vh){
			matchLog.goUp();
		}else if(y < MainView._50vh){
			matchLog.open(1);
		}else if(y < MainView._80vh){
			matchLog.open(2);
		}else{
			matchLog.goDown();
		}
		return true;
	}
	function onPreviousPage(){
		matchLog.goUp();
		return true;
	}
	function onNextPage(){
		matchLog.goDown();
		return true;
	}
}