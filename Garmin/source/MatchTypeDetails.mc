import Toybox.WatchUi;

class MatchTypeDetails extends Menu2{
	var period_time as MenuItem;
	var period_count;
	var sinbin;
	var points_try;
	var points_con;
	var points_goal;
	var clock_pk;
	var clock_con;
	var clock_restart;
	
	function initialize(){
		Menu2.initialize({:title=>Rez.Strings.details});
		period_time = new MenuItem(Rez.Strings.period_time, null, "period_time", {});
		addItem(period_time);
		period_count = new MenuItem(Rez.Strings.period_count, null, "period_count", {});
		addItem(period_count);
		sinbin = new MenuItem(Rez.Strings.sinbin, null, "sinbin", {});
		addItem(sinbin);
		points_try = new MenuItem(Rez.Strings.points_try, null, "points_try", {});
		addItem(points_try);
		points_con = new MenuItem(Rez.Strings.points_con, null, "points_con", {});
		addItem(points_con);
		points_goal = new MenuItem(Rez.Strings.points_goal, null, "points_goal", {});
		addItem(points_goal);
		clock_pk = new MenuItem(Rez.Strings.clock_pk, null, "clock_pk", {});
		addItem(clock_pk);
		clock_con = new MenuItem(Rez.Strings.clock_con, null, "clock_con", {});
		addItem(clock_con);
		clock_restart = new MenuItem(Rez.Strings.clock_restart, null, "clock_restart", {});
		addItem(clock_restart);
	}
	function onShow(){
		period_time.setSubLabel(""+MainView.main.match.period_time);
		period_count.setSubLabel(""+MainView.main.match.period_count);
		sinbin.setSubLabel(""+MainView.main.match.sinbin);
		points_try.setSubLabel(""+MainView.main.match.points_try);
		points_con.setSubLabel(""+MainView.main.match.points_con);
		points_goal.setSubLabel(""+MainView.main.match.points_goal);
		clock_pk.setSubLabel(""+MainView.main.match.clock_pk);
		clock_con.setSubLabel(""+MainView.main.match.clock_con);
		clock_restart.setSubLabel(""+MainView.main.match.clock_restart);
	}
}

class MatchTypeDetailsDelegate extends Menu2InputDelegate{
	var item_id;
	function initialize(){Menu2InputDelegate.initialize();}
	function onSelect(item){
		item_id = item.getId();
		var player_no = new PlayerNo();//reuse PlayerNo to capture a number
		pushView(player_no, new PlayerNoDelegate(player_no, null, self), SLIDE_UP);
	}
	function gotNumber(value){
		MainView.main.match.match_type = "custom";
		switch(item_id){
			case "period_time":
				MainView.main.match.period_time = value;
				MainView.main.timer_period_time = value*60;
				break;
			case "period_count":
				MainView.main.match.period_count = value;
				break;
			case "sinbin":
				MainView.main.match.sinbin = value;
				break;
			case "points_try":
				MainView.main.match.points_try = value;
				break;
			case "points_con":
				MainView.main.match.points_con = value;
				break;
			case "points_goal":
				MainView.main.match.points_goal = value;
				break;
			case "clock_pk":
				MainView.main.match.clock_pk = value;
				break;
			case "clock_con":
				MainView.main.match.clock_con = value;
				break;
			case "clock_restart":
				MainView.main.match.clock_restart = value;
				break;
		}
	}
}