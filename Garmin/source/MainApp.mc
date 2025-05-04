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