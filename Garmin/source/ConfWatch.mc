/*
 * Copyright 2020-2026 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import Toybox.Graphics;
import Toybox.WatchUi;

class ConfWatch extends View{
	var v_timer_type;
	var v_record_pens;
	var v_record_player;
	function initialize(){View.initialize();}
	function onLayout(dc as Dc) as Void{
		setLayout(Rez.Layouts.ConfWatch(dc));
		v_timer_type = findDrawableById("timer_type");
		v_record_pens = findDrawableById("record_pens");
		v_record_player = findDrawableById("record_player");
	}
	function onShow(){
		v_timer_type.setText(MainView.main.timer_type == TIMER_TYPE_UP ? Rez.Strings.up : Rez.Strings.down);
		v_record_pens.setText(MainView.main.record_pens ? Rez.Strings.on : Rez.Strings.off);
		v_record_player.setText(MainView.main.record_player ? Rez.Strings.on : Rez.Strings.off);
		requestUpdate();
	}
}

class ConfWatchDelegate extends BehaviorDelegate{
	var confWatch;
	function initialize(confWatch){
		BehaviorDelegate.initialize();
		self.confWatch = confWatch;
	}
	function onBack(){
		popView(SLIDE_UP);
		return true;
	}
	function onTap(evt){
		var y = evt.getCoordinates()[1];
		if(y < MainView._33vh){
			MainView.main.timer_type = MainView.main.timer_type == TIMER_TYPE_DOWN ? TIMER_TYPE_UP : TIMER_TYPE_DOWN;
			MainView.main.timer_type_period = MainView.main.timer_type;
		}else if(y < MainView._33vh*2){
			MainView.main.record_pens = !MainView.main.record_pens;
		}else{
			MainView.main.record_player = !MainView.main.record_player;
		}
		confWatch.onShow();
		return true;
	}
}