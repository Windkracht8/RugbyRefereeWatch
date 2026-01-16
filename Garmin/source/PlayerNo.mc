/*
 * Copyright 2020-2026 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import Toybox.Graphics;
import Toybox.WatchUi;

class PlayerNo extends View{
	enum TYPE{TYPE_DEFAULT, TYPE_LEAVE, TYPE_ENTER}
	var type = TYPE_DEFAULT;
	var v_player_no;
	var b_back;
	var placeholder = "#0";
	function initialize(){View.initialize();}
	function onLayout(dc as Dc) as Void{
		setLayout(Rez.Layouts.PlayerNo(dc));
		v_player_no = findDrawableById("player_no") as TextArea;
		b_back = findDrawableById("b_back");
		b_back.setLocation(0, b_back.locY+(MainView.height_pixels-b_back.height)/2);
		var b_done = findDrawableById("b_done");
		b_done.setLocation(MainView.width_pixels-b_done.width, b_done.locY+(MainView.height_pixels-b_done.height)/2);
		v_player_no.setText(placeholder);
	}
	function setPlayerNo(value){
		if(value > 0){
			v_player_no.setText("" + value);
			v_player_no.setColor(color_text);
			b_back.setBitmap(Rez.Drawables.ic_back);
		}else{
			v_player_no.setText(placeholder);
			v_player_no.setColor(color_disabled);
			b_back.setBitmap(Rez.Drawables.ic_back_disable);
		}
		requestUpdate();
	}
	function setType(type) as PlayerNo{
		self.type = type;
		switch(type){
			case TYPE_LEAVE:
				placeholder = "leaving";
				break;
			case TYPE_ENTER:
				placeholder = "entering";
				break;
			default:
				placeholder = "#0";
		}
		return self;
	}
}

class PlayerNoDelegate extends BehaviorDelegate{
	var event;
	var sinbin;
	var value = 0;
	var player_no;
	function initialize(player_no, event, sinbin){
		BehaviorDelegate.initialize();
		self.player_no = player_no;
		self.event = event;
		self.sinbin = sinbin;
	}
	function onBack(){
		popView(SLIDE_UP);
		return true;
	}
	function onTap(evt){
		var x = evt.getCoordinates()[0];
		var y = evt.getCoordinates()[1];
		if(y < MainView._20vh){//player_no
			return true;
		}else if(x < MainView._20vw){//b_back
			if(value == 0){return true;}
			value = Math.floor(value/10);
		}else if(x > MainView._80vw){//b_done
			if(event == null){//called to capture a number for MatchTypeDetails
				sinbin.gotNumber(value);
				popView(SLIDE_UP);
				return true;
			}
			switch(player_no.type){
				case PlayerNo.TYPE_LEAVE:
					event.who_leave = value;
					switchToView(player_no.setType(PlayerNo.TYPE_ENTER), new PlayerNoDelegate(player_no, event, null), SLIDE_UP);
					break;
				case PlayerNo.TYPE_ENTER:
					event.who_enter = value;
					popView(SLIDE_UP);
					break;
				default:
					event.who = value;
					if(sinbin != null){
						sinbin.who = value;
						if(MainView.main.match.alreadyHasYellow(event)){
							switchToView(new Confirm(Rez.Strings.second_yellow), new SecondYellowConfirmDelegate(event), SLIDE_UP);
							return true;
						}
						MainView.main.updateSinbins();
					}
					popView(SLIDE_UP);
			}
			return true;
		}else if(y < MainView._20vh*2){//1 2 3
			value *= 10;
			if(x < MainView._40vw){
				value += 1;
			}else if(x < MainView._60vw){
				value += 2;
			}else{
				value += 3;
			}
		}else if(y < MainView._20vh*3){//4 5 6
			value *= 10;
			if(x < MainView._40vw){
				value += 4;
			}else if(x < MainView._60vw){
				value += 5;
			}else{
				value += 6;
			}
		}else if(y < MainView._20vh*4){//7 8 9
			value *= 10;
			if(x < MainView._40vw){
				value += 7;
			}else if(x < MainView._60vw){
				value += 8;
			}else{
				value += 9;
			}
		}else{//0
			value *= 10;
		}
		player_no.setPlayerNo(value);
		return true;
	}
}