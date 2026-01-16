/*
 * Copyright 2020-2026 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import Toybox.Graphics;
import Toybox.WatchUi;

class FoulPlay extends View{
	function initialize(){View.initialize();}
	function onLayout(dc as Dc) as Void{setLayout(Rez.Layouts.FoulPlay(dc));}
}

class FoulPlayDelegate extends BehaviorDelegate{
	function initialize(){BehaviorDelegate.initialize();}
	function onBack(){
		popView(SLIDE_UP);
		return true;
	}
	function onTap(evt){
		var y = evt.getCoordinates()[1];
		if(y < MainView._33vh){//yellow
			popView(SLIDE_UP);
			if(MainView.main.score_delegate.isHome){
				MainView.main.yellowHome();
			}else{
				MainView.main.yellowAway();
			}
		}else if(y < MainView._33vh*2){//penalty try
			popView(SLIDE_UP);
			if(MainView.main.score_delegate.isHome){
				MainView.main.penTryHome();
			}else{
				MainView.main.penTryAway();
			}
		}else{//red
			popView(SLIDE_UP);
			if(MainView.main.score_delegate.isHome){
				MainView.main.redHome();
			}else{
				MainView.main.redAway();
			}
		}
		return true;
	}
}