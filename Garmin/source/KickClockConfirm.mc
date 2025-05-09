/*
 * Copyright 2020-2025 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import Toybox.Graphics;
import Toybox.WatchUi;

class KickClockConfirm extends View{
	var label;
	function initialize(label){
		self.label = label;
		View.initialize();
	}
	function onLayout(dc as Dc) as Void{
		setLayout(Rez.Layouts.Confirm(dc));
		var v_label = findDrawableById("confirm_label") as TextArea;
		v_label.setText(label);
	}
}
class KickClockConfirmDelegate extends BehaviorDelegate{
	var kickClockType;
	var isHome;
	function initialize(kickClockType, isHome){
		self.kickClockType = kickClockType;
		self.isHome = isHome;
		BehaviorDelegate.initialize();
	}
	function onBack(){
		popView(SLIDE_UP);
		return true;
	}
	function onTap(evt){
		if(evt.getCoordinates()[1] < MainView._50vh){return false;}
		popView(SLIDE_UP);
		if(evt.getCoordinates()[0] > MainView._50vw){
			MainView.main.kickClockConfirm(kickClockType, isHome);
		}else{
			if(isHome){
				MainView.main.kickClockHomeDone();
			}else{
				MainView.main.kickClockAwayDone();
			}
		}
		return true;
	}
}