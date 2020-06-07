var battery = navigator.battery || navigator.webkitBattery || navigator.mozBattery || null;
var timer = {
	status: "ready",
	timer: 0,
	start: 0,
	start_hold: 0,
	splitended: false,
	split: 0,
};
var match = {
	settings: {
		split_time: 40,
		split_count: 2,
		sinbin: 10,
		points_try: 5,
		points_con: 2,
		points_goal: 3
	}
	,home: {
		team: "home",
		color: "green",
		tot: 0,
		trys: 0,
		cons: 0,
		goals: 0,
		sinbins: []
	}
	,away: {
		team: "away",
		color: "red",
		tot: 0,
		trys: 0,
		cons: 0,
		goals: 0,
		sinbins: []
	}
	,events: []
}

window.onload = function () {
	document.addEventListener('tizenhwkey', function(e){
		if(e.keyName === "back"){
			try {
				var backdone = false;
				$('.overlay').each(function(){
					if($(this).is(":visible")){
						$(this).hide();
						backdone = true;
						return false;
					}
				});
				//TODO: pause/stop timer
				if(backdone === false){
					tizen.power.release("SCREEN");
					tizen.application.getCurrentApplication().exit();
				}
			} catch (ignore) {
			}
		}
	});
	document.addEventListener("rotarydetent", function(e){
		if(e.detail.direction === "CW"){
			$('body').scrollTop($('body').scrollTop() + 50);
		}else{
			$('body').scrollTop($('body').scrollTop() - 50);
		}
	});
	if(battery !== null){
		battery.addEventListener('levelchange', updateBattery);
		updateBattery();
	}else{
		$('#battery').hide();
	}
	
	updateTime();
	update();
	tizen.power.request("SCREEN", "SCREEN_NORMAL");
};

function getCurrentTimestamp(){
	var d = new Date();
	return d.getTime();
}
function timerClick(){
	switch(timer.status){
		case "ready":
			//start next split
			console.log("start next split");
			$('#bconf').hide();
			$('#stop').hide();
			timer.status = "running";
			timer.split++;
			timer.start = getCurrentTimestamp();
			logEvent("Start split " + timer.split);
			break;
		case "running":
			//hold
			console.log("hold");
			timer.status = "onhold";
			timer.start_hold = getCurrentTimestamp();
			$('#pause').show();
			logEvent("Hold");
			break;
		case "onhold":
			//resume running
			console.log("unhold");
			timer.status = "running";
			timer.start += (getCurrentTimestamp() - timer.start_hold);
			$('#pause').hide();
			logEvent("Resume");
			break;
		case "pause":
			//get ready for next split
			console.log("pause over");
			timer.status = "ready";
			updateTimer(0);
			logEvent("Pause over");
			break;
		case "stopped":
			//show event log
			showEventLog();
			break;
	}
	update();
}

function pauseClick(){
	console.log("pause");
	$('#pause').hide();
	$('#stop').show();
	timer.status = "pause";
	timer.start = getCurrentTimestamp();
	timer.splitended = false;
	$('#timer').css('color', "unset");
	logEvent("Pause start");
	logEvent("Result after split " + timer.split + ": " + match.home.tot + ":" + match.away.tot);
	if(match.events[match.events.length-3].what === "Hold"){
		match.events.splice(match.events.length-3, 1);
	}
	$.each(match.home.sinbins, function(index, value){
		value.end = value.end - timer.timer;
	});
	$.each(match.away.sinbins, function(index, value){
		value.end = value.end - timer.timer;
	});
	update();
}
function stopClick(){
	timer.status = "stopped";
	$('#stop').hide();
	$('#clear').show();
}
function clearClick(){
	updateTimer(0);
	timer.status = "ready";
	timer.split = 0;
	timer.start = 0;
	timer.start_hold = 0;
	match.home.tot = 0;
	match.home.trys = 0;
	match.home.cons = 0;
	match.home.goals = 0;
	match.home.sinbins = [];
	match.away.tot = 0;
	match.away.trys = 0;
	match.away.cons = 0;
	match.away.goals = 0;
	match.away.sinbins = [];
	updateScore();
	update();
	$('#sinbins_home').html("");
	$('#sinbins_away').html("");
	match.events = [];
	$('#clear').hide();
	$('#bconf').show();
}

function update(){
	var timerstatus = "";
	switch(timer.status){
		case "ready":
			timerstatus = "";
			break;
		case "running":
			updateTimer(getCurrentTimestamp() - timer.start);
			$('#sinbins_home').html(getSinbins(match.home.sinbins));
			$('#sinbins_away').html(getSinbins(match.away.sinbins));
			timerstatus = "";
			setTimeout(update, 50);
			break;
		case "onhold":
			timerstatus = "hold";
			break;
		case "pause":
			updateTimer(getCurrentTimestamp() - timer.start);
			timerstatus = "pause";
			setTimeout(update, 50);
			break;
	}
	$('#timerstatus').html(timerstatus);
}

function getSinbins(sinbins){
	var html = "";
	$.each(sinbins, function(index, value){
		var remaining = value.end - timer.timer;
		if(sinbins.hide === true || remaining < -(match.settings.sinbin / 2 * 60000)){
			sinbins.hide = true;
		}else if(sinbins.ended === true || remaining < 0){
			if(sinbins.ended !== true){
				beep();
				sinbins.ended = true;
			}
			html += '<span class="redtext">' + prettyTimer(0) + "</span><br>";
		}else{
			html += prettyTimer(remaining) + "<br>";
		}
	});
	return html;
}
function updateTime(){
	var d = new Date();
	var hours = d.getHours();
	var mins = d.getMinutes();
	var secs = d.getSeconds();

	var result = "";
	if(hours < 10){result += "0";}
	result += hours + ":";
	if(mins < 10){result += "0";}
	result += mins + ":";
	if(secs < 10){result += "0";}
	result += secs;
	
	$('#time').html(result);
	setTimeout(updateTime, 100);
}
function updateTimer(millisec){
	timer.timer = millisec;
	var result = prettyTimer(millisec);

	$('#timersec').html(prettyTimer(millisec));
	$('#timermil').html('.' + Math.floor((millisec % 1000) / 100));

	if(!timer.splitended && timer.status === "running" && millisec > match.settings.split_time * 60000){
		timer.splitended = true;
		$('#timer').css('color', "red");
		beep();
	}
}
function prettyTimer(millisec){
	var sec = Math.floor(millisec / 1000);
	var hours = Math.floor(sec / 3600);
	sec = sec % 3600;
	var mins = Math.floor(sec / 60);
	sec = sec % 60;
	
	var pretty = sec;
	if(sec < 10){pretty = "0" + pretty;}
	if(hours > 0 && mins < 10){
		pretty = "0" + mins + ":" + pretty;
	}else{
		pretty = mins + ":" + pretty;
	}
	if(hours > 0){pretty = hours + ":" + pretty;}

	return pretty;
}
function updateBattery(){
	var batlevelperc = Math.floor(battery.level * 100);
	$('#battery_perc').html(batlevelperc);
	if(batlevelperc < 25){
		$('#battery').css('color', 'red');
	}else{
		$('#battery').css('color', 'white');
	}
}

var team_edit = match.home;
function score_homeClick(){
	team_edit = match.home;
	score_show();
}
function score_awayClick(){
	team_edit = match.away;
	score_show();
}
function score_show(){
	if(match.settings.points_try === 0){
		$('#try').hide();
	}else{
		$('#try').show();
		$('#try').html(team_edit.trys);
	}
	if(match.settings.points_con === 0){
		$('#con').hide();
	}else{
		$('#con').show();
		$('#con').html(team_edit.cons);
	}
	if(match.settings.points_goal === 0){
		$('#goal').hide();
	}else{
		$('#goal').show();
		$('#goal').html(team_edit.goals);
	}
	$('#score').show();
}
function tryClick(){
	team_edit.trys++;
	updateScore();
	$('#score').hide();
	logEvent("TRY", team_edit);
}
function conversionClick(){
	team_edit.cons++;
	updateScore();
	$('#score').hide();
	logEvent("CONVERSION", team_edit);
}
function goalClick(){
	team_edit.goals++;
	updateScore();
	$('#score').hide();
	logEvent("GOAL", team_edit);
}
function updateScore(){
	match.home.tot = match.home.trys*match.settings.points_try + 
					match.home.cons*match.settings.points_con + 
					match.home.goals*match.settings.points_goal;
	$('#score_home').html(match.home.tot);
	match.away.tot = match.away.trys*match.settings.points_try + 
					match.away.cons*match.settings.points_con + 
					match.away.goals*match.settings.points_goal;
	$('#score_away').html(match.away.tot);
}
function cardsClick(){
	$('#card').show();
	$('#score').hide();
}

function card_yellowClick(){
	var end = timer.timer + (match.settings.sinbin*60000);
	end = Math.ceil(end/1000)*1000;
	team_edit.sinbins.push(JSON.parse('{"end":' + end + ',"ended":false,"hide":false}'));

	$('#card').hide();
	logEvent("yellow card", team_edit, $('#card_no').val());
}

function card_redClick(){
	$('#card').hide();
	logEvent("red card", team_edit, $('#card_no').val());
}

function correctClick(){
	var html = "";
	$.each(match.events, function(index, value){
		if(value.team !== team_edit.team){return;}
		html += '<span onclick="removeEvent(\'' + index + '\')">';
		html += value.timer;
		html += ' ' + value.what;
		if(value.who){
			html += ' ' + value.who;
		}
		html += '</span><br>';
	});

	if(html === ""){
		html = "nothing to correct for " + team_edit.team;
	}
	$('#correct').html(html);
	$('#correct').show();
	$('#score').hide();
}
function removeEvent(index){
	console.log(match.events);
	switch(match.events[index].what){
		case "TRY":
			team_edit.trys--;
			break;
		case "CONVERSION":
			team_edit.cons--;
			break;
		case "GOAL":
			team_edit.goals--;
			break;
		case "yellow card":
			//TODO: remove from sinbins
			break;
	}
	match.events.splice(index, 1);
	
	updateScore();
	$('#correct').hide();
}
function bconfClick(){
	$('#color_home').css('background', match.home.color);
	$('#color_home').val(match.home.color);
	$('#color_away').css('background', match.away.color);
	$('#color_away').val(match.away.color);

	$('#split_time').val(match.settings.split_time);
	$('#split_count').val(match.settings.split_count);
	$('#sinbin').val(match.settings.sinbin);
	
	$('#conf').show();
}
function color_homeChange(){
	match.home.color = $('#color_home').val();
	$('#color_home').css('background', match.home.color);
	$('#home').css('background', match.home.color);
}
function color_awayChange(){
	match.away.color = $('#color_away').val();
	$('#color_away').css('background', match.away.color);
	$('#away').css('background', match.away.color);
}
function split_timeChange(){
	match.settings.split_time = $('#split_time').val();
}
function split_countChange(){
	match.settings.split_count = $('#split_count').val();
}
function sinbinChange(){
	match.settings.sinbin = $('#sinbin').val();
}

function logEvent(what, team = false, who = false){
	var currenttimer = timer.timer + ((timer.split-1)*match.settings.split_time*60000);
	var temp = '{"time":"' + $('#time').html() + '",' +
				'"timer":"' + prettyTimer(currenttimer) + '",';
	if(team !== false){
		temp +=	'"team":"' + team.team + '",';
	}
	if(who !== false){
		temp +=	'"who":"' + who + '",';
	}
	temp +=	'"what":"' + what + '"';
	temp += '}';
	console.log(JSON.parse(temp));
	match.events.push(JSON.parse(temp));
}

function showEventLog(){
	var html = "";
	$.each(match.events, function(index, value){
		html += value.time;
		html += " " + value.timer;
		html += " " + value.what;
		if(value.team){
			html += " " + value.team;
		}
		if(value.who){
			html += " " + value.who;
		}
		html += "<br>";
	});
	
	$('#events').html(html);
	$('#events').show();
}

function incomingSettings(settings){
	if(timer.status !== "ready"){
		console.log("not ready for settings");
		return false;
	}
	//TODO: also receive match_type
	//"15s", "10s", "7s", "beach 7s", "beach 5s"
	
	match.home.team = settings.home_name;
	match.away.team = settings.away_name;
	match.home.color = settings.home_color;
	match.away.color = settings.away_color;
	match.settings.split_time = settings.split_time;
	match.settings.split_count = settings.split_count;
	match.settings.sinbin = settings.sinbin;
	match.settings.points_try = settings.points_try;
	match.settings.points_con = settings.points_con;
	match.settings.points_goal = settings.points_goal;
	
	$('#home').css('background', match.home.color);
	$('#away').css('background', match.away.color);

	return true;
}
function beep(){
	console.log("beep");
	navigator.vibrate([500, 500, 500]);
}

function showMessage(message){
	console.log(message);
	$('#message').append(message);
	$('#message').show();
}
