/*
 * Copyright 2020-2025 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import Toybox.Application;
import Toybox.Lang;
import Toybox.WatchUi;

class MainApp extends AppBase{
	function initialize(){AppBase.initialize();}
	function onStart(state as Dictionary?) as Void{}
	function onStop(state as Dictionary?) as Void{}
	function getInitialView() as [Views] or [Views, InputDelegates]{
		Utils.setBootTime();
		FileStore.read();
		return[new MainView(), new MainDelegate()];
	}
}

function getApp() as MainApp{return getApp() as MainApp;}