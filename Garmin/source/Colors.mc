import Toybox.WatchUi;

class Colors extends Menu2{
	function initialize(){
		Menu2.initialize({:title=>Rez.Strings.details});
		addItem(new MenuItem("black", null, "black", {}));
		addItem(new MenuItem("blue", null, "blue", {}));
		addItem(new MenuItem("brown", null, "brown", {}));
		addItem(new MenuItem("gold", null, "gold", {}));
		addItem(new MenuItem("green", null, "green", {}));
		addItem(new MenuItem("orange", null, "orange", {}));
		addItem(new MenuItem("pink", null, "pink", {}));
		addItem(new MenuItem("purple", null, "purple", {}));
		addItem(new MenuItem("red", null, "red", {}));
		addItem(new MenuItem("white", null, "white", {}));
	}
}

class ColorsDelegate extends MenuInputDelegate{
	var isHome;
	function initialize(isHome){
		MenuInputDelegate.initialize();
		self.isHome = isHome;
	}
	function onSelect(item){
		if(isHome){
			MainView.main.match.home.color = item.getId();
		}else{
			MainView.main.match.away.color = item.getId();
		}
		popView(SLIDE_UP);
	}
}