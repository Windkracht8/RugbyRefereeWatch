import Toybox.Graphics;
import Toybox.WatchUi;

class Score extends View{
	static var hide_con;
	static var hide_goal;
	function initialize(){
		View.initialize();
		hide_con = MainView.main.match.points_con == 0;
		hide_goal = MainView.main.match.points_goal == 0;
	}
	
	function onLayout(dc as Dc) as Void{
		setLayout(Rez.Layouts.Score(dc));
		if(hide_con && hide_goal){
			var itemHeight = MainView._50vh;
			resizeView("score_try", itemHeight, 0);
			hideView("score_con");
			hideView("score_goal");
			resizeView("foul_play", itemHeight, itemHeight);
		}else if(hide_con){
			var itemHeight = Math.floor(MainView._33vh);
			resizeView("score_try", itemHeight, 0);
			hideView("score_con");
			resizeView("score_goal", itemHeight, itemHeight);
			resizeView("foul_play", itemHeight, itemHeight*2);
		}else if(hide_goal){
			var itemHeight = Math.floor(MainView._33vh);
			resizeView("score_try", itemHeight, 0);
			resizeView("score_con", itemHeight, itemHeight);
			hideView("score_goal");
			resizeView("foul_play", itemHeight, itemHeight*2);
		}
	}
	function hideView(viewId){
		var view = findDrawableById(viewId);
		view.setVisible(false);
	}
	function resizeView(viewId, itemHeight, y){
		var view = findDrawableById(viewId);
		view.setSize(MainView.width_pixels, itemHeight);
		view.setLocation(0, y);
	}
}
class ScoreDelegate extends BehaviorDelegate{
	var isHome;
	var itemHeight;
	var end_y_con;
	var end_y_goal;
	
	function initialize(){
		BehaviorDelegate.initialize();
		itemHeight = MainView._25vh;
		if(Score.hide_con && Score.hide_goal){
			itemHeight = MainView._50vh;
			end_y_con = itemHeight;
			end_y_goal = itemHeight;
		}else if(Score.hide_con){
			itemHeight = Math.floor(MainView._33vh);
			end_y_con = itemHeight;
			end_y_goal = itemHeight*2;
		}else if(Score.hide_goal){
			itemHeight = Math.floor(MainView._33vh);
			end_y_con = itemHeight*2;
			end_y_goal = itemHeight*2;
		}else{
			end_y_con = itemHeight*2;
			end_y_goal = itemHeight*3;
		}
	}
	function onBack(){
		System.println("ScoreDelegate.onBack");
		popView(SLIDE_UP);
		return true;
	}
	function onTap(evt){
		var y = evt.getCoordinates()[1];
		if(y < itemHeight){//try
			popView(SLIDE_UP);
			if(isHome){
				MainView.main.tryHome();
			}else{
				MainView.main.tryAway();
			}
		}else if(y < end_y_con){//con
			popView(SLIDE_UP);
			if(isHome){
				MainView.main.conHome();
			}else{
				MainView.main.conAway();
			}
		}else if(y < end_y_goal){//goal
			popView(SLIDE_UP);
			if(isHome){
				MainView.main.goalHome();
			}else{
				MainView.main.goalAway();
			}
		}else{//foul play
			switchToView(MainView.main.foul_play, MainView.main.foul_play_delegate, SLIDE_UP);
		}
		return true;
	}
}