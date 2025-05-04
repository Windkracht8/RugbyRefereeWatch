import Toybox.Graphics;
import Toybox.WatchUi;

class PlayerNo extends View{
	var v_player_no;
	var b_back;
	function initialize(){View.initialize();}
	function onLayout(dc as Dc) as Void{
		setLayout(Rez.Layouts.PlayerNo(dc));
		v_player_no = findDrawableById("player_no");
		b_back = findDrawableById("b_back");
		b_back.setLocation(0, b_back.locY+(MainView.height_pixels-b_back.height)/2);
		var b_done = findDrawableById("b_done");
		b_done.setLocation(MainView.width_pixels-b_done.width, b_done.locY+(MainView.height_pixels-b_done.height)/2);
	}
	function setPlayerNo(value){
		v_player_no.setText("" + value);
		if(value > 0){
			v_player_no.setColor(color_text);
			b_back.setBitmap(Rez.Drawables.ic_back);
		}else{
			v_player_no.setColor(color_disabled);
			b_back.setBitmap(Rez.Drawables.ic_back_disable);
		}
		requestUpdate();
	}
}

class PlayerNoDelegate extends BehaviorDelegate{
	var event;
	var sinbin;
	var value = 0;
	var v_player_no;
	function initialize(v_player_no, event, sinbin){
		BehaviorDelegate.initialize();
		self.v_player_no = v_player_no;
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
			event.who = value;
			if(sinbin != null){sinbin.who = value;}
			MainView.main.updateSinbins();
			popView(SLIDE_UP);
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
		v_player_no.setPlayerNo(value);
		return true;
	}
}