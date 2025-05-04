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