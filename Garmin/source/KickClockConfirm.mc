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
		if(evt.getCoordinates()[0] > MainView._50vw){
			MainView.main.kickClockConfirm(kickClockType, isHome);
			popView(SLIDE_UP);
		}else{
			if(isHome){
				MainView.main.kickClockHomeDone();
			}else{
				MainView.main.kickClockAwayDone();
			}
			popView(SLIDE_UP);
		}
		return true;
	}
}