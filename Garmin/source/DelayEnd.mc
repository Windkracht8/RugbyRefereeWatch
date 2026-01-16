/*
 * Copyright 2020-2026 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import Toybox.Graphics;
import Toybox.WatchUi;

class DelayEnd extends View{
	var v_progress;
	var value = 0;
	var start_time;
	enum DELAY_TYPE{DELAY_END, DELAY_FINISH}
	var delay_type;
	function initialize(start_time, delay_type){
		View.initialize();
		self.start_time = start_time;
		self.delay_type = delay_type;
	}
	function onLayout(dc as Dc) as Void{
		setLayout(Rez.Layouts.DelayEnd(dc));
		v_progress = findDrawableById("progress") as Bitmap;
	}
	function progress(){
		if(value == 8){
			if(delay_type == DELAY_END){
				MainView.main.delay_end_finish(start_time);
			}else{
				MainView.main.delay_finish_finish(start_time);
			}
			popView(SLIDE_UP);
		}
		value++;
		switch(value){
			case 1:
				v_progress.setBitmap(Rez.Drawables.circle_1);
				break;
			case 2:
				v_progress.setBitmap(Rez.Drawables.circle_2);
				break;
			case 3:
				v_progress.setBitmap(Rez.Drawables.circle_3);
				break;
			case 4:
				v_progress.setBitmap(Rez.Drawables.circle_4);
				break;
			case 5:
				v_progress.setBitmap(Rez.Drawables.circle_5);
				break;
			case 6:
				v_progress.setBitmap(Rez.Drawables.circle_6);
				break;
			case 7:
				v_progress.setBitmap(Rez.Drawables.circle_7);
				break;
			case 8:
				v_progress.setBitmap(Rez.Drawables.circle_8);
				break;
		}
	}
}

class DelayEndDelegate extends BehaviorDelegate{
	function initialize(){BehaviorDelegate.initialize();}
	function onBack(){
		popView(SLIDE_UP);
		return true;
	}
	function onTap(evt){
		if(evt.getCoordinates()[1] > MainView._50vh){onBack();}
		return true;
	}
}
