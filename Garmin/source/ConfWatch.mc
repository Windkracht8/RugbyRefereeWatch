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