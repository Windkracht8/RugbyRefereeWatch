/*
 * Copyright 2020-2026 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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

class ColorsDelegate extends Menu2InputDelegate{
	var isHome;
	function initialize(isHome){
		Menu2InputDelegate.initialize();
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
