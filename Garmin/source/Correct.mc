import Toybox.WatchUi;

class Correct extends Menu2{
	(:typecheck(false))//Check fails on MainView.main.match.events
	function initialize(){
		Menu2.initialize({:title=>Rez.Strings.correct});
		var empty = true;
		for(var i=MainView.main.match.events.size()-1; i>=0; i--){
			var event = MainView.main.match.events[i];
			if(event.what.equals("TRY") ||
				event.what.equals("CONVERSION") ||
				event.what.equals("PENALTY TRY") ||
				event.what.equals("PENALTY") ||
				event.what.equals("GOAL") ||
				event.what.equals("YELLOW CARD") ||
				event.what.equals("RED CARD")
			){
				var label = Utils.prettyTimer(event.timer) + " " + event.what;
				if(event.team != null){
					label += " " + event.team;
					if(event.who > 0){
						label += " " + event.who;
					}
				}
				if(event.deleted){
					addItem(new MenuItem(label, Rez.Strings.deleted, event.id, {}));
				}else{
					addItem(new MenuItem(label, null, event.id, {}));
				}
				empty = false;
			}
		}
		if(empty){
			addItem(new MenuItem(Rez.Strings.empty, null, "", {}));
		}
	}
}

class CorrectDelegate extends Menu2InputDelegate{
	function initialize(){Menu2InputDelegate.initialize();}
	function onSelect(item){
		if(item.getId().toString().length() > 1){MainView.main.match.delEvent(item.getId());}
		popView(SLIDE_UP);
	}
}