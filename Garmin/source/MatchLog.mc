import Toybox.Graphics;
import Toybox.WatchUi;

class MatchLog extends View{
	var v_up;
	var v_down;
	var v_item_1_date;
	var v_item_1_teams;
	var v_item_1_result;
	var v_item_2_date;
	var v_item_2_teams;
	var v_item_2_result;
	var match_ids;
	var size = 0;
	var index = 0;

	function initialize(){
		View.initialize();
		match_ids = FileStore.match_ids.reverse();
		size = ((match_ids.size()-1)/2)+1;
	}
	function onLayout(dc as Dc) as Void{
		setLayout(Rez.Layouts.MatchLog(dc));
		v_up = findDrawableById("up");
		v_down = findDrawableById("down");
		v_item_1_date = findDrawableById("item_1_date");
		v_item_1_teams = findDrawableById("item_1_teams");
		v_item_1_result = findDrawableById("item_1_result");
		v_item_2_date = findDrawableById("item_2_date");
		v_item_2_teams = findDrawableById("item_2_teams");
		v_item_2_result = findDrawableById("item_2_result");
		go(0);
	}
	(:typecheck(false))//Check fails on FileStore.match_ids
	function go(Index){
		index = Index;
		var match = FileStore.readMatch(match_ids[index*2]);
		v_item_1_date.setText(Utils.prettyTime(match.match_id/1000));
		v_item_1_teams.setText(match.home.team + " : " + match.away.team);
		v_item_1_result.setText(match.home.tot + " : " + match.away.tot);

		if(match_ids.size()>=(index*2+1)){
			match = FileStore.readMatch(match_ids[index*2+1]);
			v_item_2_date.setText(Utils.prettyTime(match.match_id/1000));
			v_item_2_teams.setText(match.home.team + " : " + match.away.team);
			v_item_2_result.setText(match.home.tot + " : " + match.away.tot);
		}else{
			v_item_2_date.setText("");
			v_item_2_teams.setText("");
			v_item_2_result.setText("");
		}

		v_up.setVisible(index > 0);
		v_down.setVisible(size > index+1);
		requestUpdate();
	}
	function goDown(){
		if(index+1 >= size){return;}
		go(index+1);
	}
	function goUp(){
		if(index <= 0){return;}
		go(index-1);
	}
	(:typecheck(false))//Check fails on FileStore.match_ids
	function open(item){
		if(match_ids.size()<index*2+item){return;}
		var report = new Report(FileStore.readMatch(match_ids[index*2+item-1]));
		pushView(report, new ReportDelegate(report), SLIDE_UP);
	}
}

class MatchLogDelegate extends BehaviorDelegate{
	var matchLog;
	function initialize(matchLog){
		BehaviorDelegate.initialize();
		self.matchLog = matchLog;
	}
	function onBack(){
		popView(SLIDE_UP);
		return true;
	}
	function onTap(evt){
		if(evt.getCoordinates()[0] < MainView._15vw){
			onBack();
			return true;
		}
		var y = evt.getCoordinates()[1];
		if(y < MainView._20vh){
			matchLog.goUp();
		}else if(y < MainView._50vh){
			matchLog.open(1);
		}else if(y < MainView._80vh){
			matchLog.open(2);
		}else{
			matchLog.goDown();
		}
		return true;
	}
	function onPreviousPage(){
		matchLog.goUp();
		return true;
	}
	function onNextPage(){
		matchLog.goDown();
		return true;
	}
}