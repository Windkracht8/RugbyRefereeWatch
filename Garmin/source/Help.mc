import Toybox.Graphics;
import Toybox.Lang;
import Toybox.WatchUi;

class Help extends View{
	var v_up;
	var v_down;
	var v_title;
	var v_content;

	var titles as Array<String> = [
		"Kick clock",
		"Custom matches",
		"Penalty try",
		"Corrections",
		"Kick off",
		"Storage",
		"Sync with phone",
		"Get help"
	];
	var contents as Array<String> = [
		"A kick clock starts when you enter a try or penalty. The clock hides automatically. You can tap the clock to confirm success or miss.",
		"In the settings you can select the standard match types. If you want to change anything (like duration), tap \'Match type details\'.",
		"Looking for penalty try? Check under foul play.",
		"During a game, swipe back to open the correction screen. On this screen you can tap the event you want to undo.",
		"Before the game starts you can set which team will kick off by tapping the home or away team score.",
		"The last 10 matches will be stored on the watch.",
		"Sorry, this is not implemented yet.",//When implemented, update the other help items to include the features
		"From the settings you can open this screen. Find support contacts in the App Store for more help."
	];
	var size = -1;
	var index = -1;
	static var showWelcome = false;

	function initialize(){View.initialize();}
	function onLayout(dc as Dc) as Void{
		setLayout(Rez.Layouts.Help(dc));
		size = titles.size();
		v_up = findDrawableById("up");
		v_down = findDrawableById("down");
		v_title = findDrawableById("title");
		v_content = findDrawableById("content");
		if(showWelcome){
			v_title.setText("Welcome");
			v_content.setText("Here are a few tips to get you familiar with this app.");
		}else{
			go(0);
		}
	}
	function go(index){
		self.index = index;
		v_title.setText(titles[index]);
		v_content.setText(contents[index]);
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
}

class HelpDelegate extends BehaviorDelegate{
	var help;
	function initialize(help){
		BehaviorDelegate.initialize();
		self.help = help;
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
		if(y < MainView._25vh){
			help.goUp();
		}else if(y > MainView._75vh){
			help.goDown();
		}
		return true;
	}
	function onPreviousPage(){
		help.goUp();
		return true;
	}
	function onNextPage(){
		help.goDown();
		return true;
	}
}