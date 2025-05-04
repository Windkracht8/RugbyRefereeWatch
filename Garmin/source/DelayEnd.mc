import Toybox.Graphics;
import Toybox.WatchUi;

class DelayEnd extends View{
	var v_progress;
	var value = 0;
	var start_time;
	function initialize(start_time){
		View.initialize();
		self.start_time = start_time;
	}
	function onLayout(dc as Dc) as Void{
		setLayout(Rez.Layouts.DelayEnd(dc));
		v_progress = findDrawableById("progress") as Bitmap;
	}
	function progress(){
		if(value == 8){
			MainView.main.delay_end_finish(start_time);
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