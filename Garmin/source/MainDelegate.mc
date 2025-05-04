import Toybox.WatchUi;

class MainDelegate extends BehaviorDelegate{
	var main;
	function initialize(){
		BehaviorDelegate.initialize();
		main = MainView.main;
	}

	function onBack(){
		if(main.timer_status == STATUS_CONF){
			System.exit();
		}else{
			pushView(new Correct(), new CorrectDelegate(), SLIDE_UP);
		}
		return true;
	}

	function onTap(evt){
		var y = evt.getCoordinates()[1];
		if(y < MainView._25vh){
			onConfWatchTap();
		}else if(y < MainView._50vh){
			var x = evt.getCoordinates()[0];
			if(x < MainView._50vw){
				onHomeTap();
			}else{
				onAwayTap();
			}
		}else if(y < MainView._75vh){
			var x = evt.getCoordinates()[0];
			if(x < MainView._50vw){
				onOverTimerLeftTap();
			}else{
				onOverTimerRightTap();
			}
		}else if(y < MainView._85vh){
			onBottomTap();
		}else{
			var x = evt.getCoordinates()[0];
			if(x < MainView._50vw){
				onPenHomeTap();
			}else{
				onPenAwayTap();
			}
		}
		return true;
	}

	function onConfWatchTap(){
		switch(main.timer_status){
			case STATUS_TIME_OFF:
			case STATUS_REST:
				pushView(main.conf_watch, main.conf_watch_delegate, SLIDE_UP);
		}
	}
	function onHomeTap(){
		switch(main.timer_status){
			case STATUS_CONF:
				main.kickoffHome();
				break;
			case STATUS_RUNNING:
			case STATUS_TIME_OFF:
			case STATUS_REST:
				main.score_delegate.isHome = true;
				pushView(main.score, main.score_delegate, SLIDE_UP);
		}
	}
	function onAwayTap(){
		switch(main.timer_status){
			case STATUS_CONF:
				main.kickoffAway();
				break;
			case STATUS_RUNNING:
			case STATUS_TIME_OFF:
			case STATUS_REST:
				main.score_delegate.isHome = false;
				pushView(main.score, main.score_delegate, SLIDE_UP);
		}
	}
	function onTimerTap(){
		if(main.timer_status != STATUS_RUNNING){return;}
		main.timeOff();
	}
	function onOverTimerLeftTap(){
		if(main.timer_status == STATUS_CONF){
			main.start();
		}else if(main.kickClockType_home != KICK_CLOCK_TYPE_NONE){
			main.kickClockHomeClick();
		}else{
			onOverTimerTap();
		}
	}
	function onOverTimerRightTap(){
		if(main.timer_status == STATUS_CONF){
			var matchLog = new MatchLog();
			pushView(matchLog, new MatchLogDelegate(matchLog), SLIDE_UP);
		}else if(main.kickClockType_away != KICK_CLOCK_TYPE_NONE){
			main.kickClockAwayClick();
		}else{
			onOverTimerTap();
		}
	}
	function onOverTimerTap(){
		switch(main.timer_status){
			case STATUS_RUNNING:
				onTimerTap();
				break;
			case STATUS_TIME_OFF:
				main.resume();
				break;
			case STATUS_REST:
				main.nextPeriod();
				break;
			case STATUS_READY:
				main.start();
				break;
			case STATUS_FINISHED:
				var report = new Report(main.match);
				pushView(report, new ReportDelegate(report), SLIDE_UP);
		}
	}
	function onBottomTap(){
		switch(main.timer_status){
			case STATUS_CONF:
				pushView(new Conf(), new ConfDelegate(), SLIDE_UP);
				break;
			case STATUS_RUNNING:
				onTimerTap();
				break;
			case STATUS_TIME_OFF:
				main.delay_end_start();
				break;
			case STATUS_REST:
				main.finish();
				break;
			case STATUS_FINISHED:
				main.clear();
		}
	}
	function onPenHomeTap(){
		if(main.timer_status == STATUS_RUNNING){
			main.penHome();
		}else{
			onBottomTap();
		}
	}
	function onPenAwayTap(){
		if(main.timer_status == STATUS_RUNNING){
			main.penAway();
		}else{
			onBottomTap();
		}
	}
}