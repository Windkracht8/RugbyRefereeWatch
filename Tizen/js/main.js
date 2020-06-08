var battery = navigator.battery || navigator.webkitBattery || navigator.mozBattery || null;
var timer = {
	status: "conf",
	timer: 0,
	start: 0,
	start_timeoff: 0,
	periodended: false,
	period: 0,
};
var match = {
	settings: {
		period_time: 40,
		period_count: 2,
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
				if(backdone === false){
					if(timer.status !== "conf" || timer.status !== "finished"){
						correctShow();
					}else{
						tizen.power.release("SCREEN");
						tizen.application.getCurrentApplication().exit();
					}
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
	if(timer.status === "running"){
		//time off
		timer.status = "timeoff";
		timer.start_timeoff = getCurrentTimestamp();
		logEvent("Time off");
		update();
	}
}

function bresumeClick(){
	switch(timer.status){
		case "conf":
		case "ready":
			//start next period
			timer.status = "running";
			timer.period++;
			timer.start = getCurrentTimestamp();
			logEvent("Start " + getPeriodName());
			update();
			break;
		case "timeoff":
			//resume running
			timer.status = "running";
			timer.start += (getCurrentTimestamp() - timer.start_timeoff);
			logEvent("Resume time");
			update();
			break;
		case "rest":
			//get ready for next period
			timer.status = "ready";
			updateTimer(0);
			logEvent("Rest over");
			break;
	}	
}
function brestClick(){
	timer.status = "rest";
	timer.start = getCurrentTimestamp();
	timer.periodended = false;
	$('#timer').css('color', "unset");
	$.each(match.home.sinbins, function(index, value){
		value.end = value.end - timer.timer;
	});
	$.each(match.away.sinbins, function(index, value){
		value.end = value.end - timer.timer;
	});
	update();
	
	if(match.events[match.events.length-1].what === "Time off"){
		match.events.splice(match.events.length-1, 1);
	}
	logEvent("Result " + getPeriodName() + " " + match.home.tot + ":" + match.away.tot);
	logEvent("Rest start");
}
function bfinishClick(){
	timer.status = "finished";
	update();

	if(match.events[match.events.length-1].what === "Rest start"){
		match.events.splice(match.events.length-1, 1);
	}
}
function bclearClick(){
	updateTimer(0);
	timer.status = "conf";
	timer.period = 0;
	timer.start = 0;
	timer.start_timeoff = 0;
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
	$('#bconf').show();
}

function update(){
	var timerstatus = "";
	$('.bottombutton').each(function (){$(this).hide();});
	switch(timer.status){
		case "conf":
			$('#bconf').show();
		case "ready":
			$('#bstart').show();
			timerstatus = "";
			break;
		case "running":
			updateTimer(getCurrentTimestamp() - timer.start);
			updateSinbins();
			timerstatus = "";
			setTimeout(update, 50);
			break;
		case "timeoff":
			$('#bresume').show();
			$('#brest').show();
			timerstatus = "time off";
			break;
		case "rest":
			$('#bnext').show();
			$('#bfinish').show();
			updateTimer(getCurrentTimestamp() - timer.start);
			timerstatus = "rest";
			setTimeout(update, 50);
			break;
		case "finished":
			$('#breport').show();
			$('#bclear').show();
			timerstatus = "finished";
			break;
	}
	$('#timerstatus').html(timerstatus);
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

	if(!timer.periodended && timer.status === "running" && millisec > match.settings.period_time * 60000){
		timer.periodended = true;
		$('#timer').css('color', "red");
		beep();
	}
}
function updateSinbins(){
	$('#sinbins_home').html(getSinbins(match.home.sinbins));
	$('#sinbins_away').html(getSinbins(match.away.sinbins));
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
	$('#try').html(team_edit.trys);
	$('#con').html(team_edit.cons);
	$('#goal').html(team_edit.goals);

	match.settings.points_try === 0 ? $('#score_try').hide() : $('#score_try').show();
	match.settings.points_con === 0 ? $('#score_con').hide() : $('#score_con').show();
	match.settings.points_goal === 0 ? $('#score_goal').hide() : $('#score_goal').show();
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
	var id = logEvent("yellow card", team_edit, $('#card_no').val());

	var end = timer.timer + (match.settings.sinbin*60000);
	end = Math.ceil(end/1000)*1000;
	team_edit.sinbins.push(JSON.parse('{"end":' + end + ',"ended":false,"hide":false,"id":' + id + '}'));

	$('#card').hide();
	updateSinbins();
}

function card_redClick(){
	$('#card').hide();
	logEvent("red card", team_edit, $('#card_no').val());
}

function correctShow(){
	var html = "";
	$.each(match.events, function(index, value){
		if(value.what !== "TRY" &&
			value.what !== "CONVERSION" &&
			value.what !== "GOAL" &&
			value.what !== "yellow card"
			//TODO: also support cancel of timer status changes
		){
			return;
		}
		html += '<span onclick="removeEvent(\'' + index + '\')">';
		html += value.timer;
		html += ' ' + value.what;
		if(value.team){
			html += ' ' + value.team;
		}
		if(value.who){
			html += ' ' + value.who;
		}
		html += '</span><br>';
	});

	if(html === ""){
		html = "nothing to correct ";
	}
	$('#correct').html(html);
	$('#correct').show();
}
function removeEvent(index){
	team_edit = match.events[index].team === match.home.team ? match.home : match.away;
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
			var id = match.events[index].id;
			$.each(team_edit.sinbins, function (index, value){
				if(value.id === id){
					team_edit.sinbins.splice(index, 1);
				}
			});
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

	$('#period_time').val(match.settings.period_time);
	$('#period_count').val(match.settings.period_count);
	$('#sinbin').val(match.settings.sinbin);

	$('#points_try').val(match.settings.points_try);
	$('#points_con').val(match.settings.points_con);
	$('#points_goal').val(match.settings.points_goal);
	
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
function match_typeChange(){
	switch($('#match_type').val()){
		case "15s":
			$('#period_time').val("40");
			$('#period_count').val("2");
			$('#sinbin').val("10");
			$('#points_try').val("5");
			$('#points_con').val("2");
			$('#points_goal').val("3");
			break;
		case "10s":
			$('#period_time').val("10");
			$('#period_count').val("2");
			$('#sinbin').val("2");
			$('#points_try').val("5");
			$('#points_con').val("2");
			$('#points_goal').val("3");
			break;
		case "7s":
			$('#period_time').val("7");
			$('#period_count').val("2");
			$('#sinbin').val("2");
			$('#points_try').val("5");
			$('#points_con').val("2");
			$('#points_goal').val("3");
			break;
		case "beach 7s":
			$('#period_time').val("7");
			$('#period_count').val("2");
			$('#sinbin').val("2");
			$('#points_try').val("1");
			$('#points_con').val("0");
			$('#points_goal').val("0");
			break;
		case "beach 5s":
			$('#period_time').val("5");
			$('#period_count').val("2");
			$('#sinbin').val("2");
			$('#points_try').val("1");
			$('#points_con').val("0");
			$('#points_goal').val("0");
			break;
	}
	period_timeChange();
	period_countChange();
	sinbinChange();
	points_tryChange();
	points_conChange();
	points_goalChange();
}
function period_timeChange(){
	match.settings.period_time = parseInt($('#period_time').val());
}
function period_countChange(){
	match.settings.period_count = parseInt($('#period_count').val());
}
function sinbinChange(){
	match.settings.sinbin = parseInt($('#sinbin').val());
}
function points_tryChange(){
	match.settings.points_try = parseInt($('#points_try').val());
}
function points_conChange(){
	match.settings.points_con = parseInt($('#points_con').val());
}
function points_goalChange(){
	match.settings.points_goal = parseInt($('#points_goal').val());
}


function getPeriodName(){
	if(match.settings.period_count === 2){
		switch(timer.period){
			case 1:
				return "first half";
			case 2:
				return "second half";
			default:
				return "extra time";
		}
	}
	return "period " + timer.period;
}

function logEvent(what, team = false, who = false){
	var id = Date.now();
	var currenttimer = timer.timer + ((timer.period-1)*match.settings.period_time*60000);
	var temp = '{"time":"' + $('#time').html() + '",' +
				'"timer":"' + prettyTimer(currenttimer) + '",';
	if(team !== false){
		temp +=	'"team":"' + team.team + '",';
	}
	if(who !== false){
		temp +=	'"who":"' + who + '",';
	}
	temp +=	'"what":"' + what + '",';
	temp +=	'"id":' + id;
	temp += '}';
	console.log(JSON.parse(temp));
	match.events.push(JSON.parse(temp));
	return id;
}

function showReport(){
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
	
	$('#report').html(html);
	$('#report').show();
}

function incomingSettings(settings){
	if(timer.status !== "conf"){
		console.log("not ready for settings");
		return false;
	}
	
	match.home.team = settings.home_name;
	match.away.team = settings.away_name;
	match.home.color = settings.home_color;
	match.away.color = settings.away_color;
	match.settings.period_time = settings.period_time;
	match.settings.period_count = settings.period_count;
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
