/* global $, file_init, file_storeMatch, file_storeSettings */
/* exported timerClick, bresumeClick, brestClick, bfinishClick, bclearClick, bconfwatchClick, score_homeClick, score_awayClick, tryClick, conversionClick, goalClick, cardsClick, card_yellowClick, card_redClick, bconfClick, color_homeChange, color_awayChange, match_typeChange, incomingSettings, getSettings, settingsRead, removeEvent, record_playerChange, screen_onChange, timer_typeChange, showReport, showMessage */

var timer = {
	status: "conf",
	timer: 0,
	start: 0,
	start_timeoff: 0,
	periodended: false,
	period: 0
};
var match = {
	settings: {
		match_type: "15s",
		period_time: 40,
		period_count: 2,
		sinbin: 10,
		points_try: 5,
		points_con: 2,
		points_goal: 3,
		record_player: 0,
		screen_on: 1,
		timer_type: 1//0:up, 1:down
	},
	home: {
		id: "home",
		team: "home",
		color: "green",
		tot: 0,
		tries: 0,
		cons: 0,
		goals: 0,
		sinbins: [],
		kickoff: 0
	},
	away: {
		id: "away",
		team: "away",
		color: "red",
		tot: 0,
		tries: 0,
		cons: 0,
		goals: 0,
		sinbins: [],
		kickoff: 0
	},
	events: [],
	matchid: 0
};

window.onload = function () {
	document.addEventListener('tizenhwkey', function(e){
		if(e.keyName === "back"){
			back();
		}
	});
	document.addEventListener("rotarydetent", function(e){
		if(e.detail.direction === "CW"){
			$('body').scrollTop($('body').scrollTop() + 50);
		}else{
			$('body').scrollTop($('body').scrollTop() - 50);
		}
	});
	try{
		tizen.systeminfo.addPropertyValueChangeListener("BATTERY", updateBattery);
		tizen.systeminfo.getPropertyValue("BATTERY", updateBattery);
	}catch(e){
		console.log("tizen.systeminfo exception " + e.message);
	}
	file_init();

	update();
	updateButtons();
	record_playerChanged();

	function onScreenStateChanged(previousState, newState) {
	    if(newState === 'SCREEN_NORMAL'){
	    	requestScreen_on();
	    }
	}
	try{
		tizen.power.setScreenStateChangeListener(onScreenStateChanged);
	}catch(e){
		console.log("setScreenStateChangeListener exception " + e.message);
	}
};
 
function back(){
	try {
		var backdone = false;
		$('.overlay').each(function(){
			if($(this).is(":visible")){
				$(this).hide();
				if($(this).attr("id") === "conf"){
					file_storeSettings(match.settings);
				}
				backdone = true;
				return false;
			}
		});
		if(backdone === false){
			if(timer.status !== "conf" && timer.status !== "finished"){
				correctShow();
			}else{
				tizen.power.release("SCREEN");
				tizen.application.getCurrentApplication().exit();
			}
		}
	} catch (e) {
		console.log("back exception " + e.message);
	}
}
function getCurrentTimestamp(){
	var d = new Date();
	return d.getTime();
}

function timerClick(){
	if(timer.status === "running"){
		//time off
		singleBeep();
		timer.status = "timeoff";
		timer.start_timeoff = getCurrentTimestamp();
		logEvent("Time off", null, null);
		updateButtons();
		startTimeOffBuzz();
	}
}

function startTimeOffBuzz(){
	setTimeout(timeOffBuzz, 15000);
}
function timeOffBuzz(){
	if(timer.status === "timeoff"){
		beep();
		startTimeOffBuzz();
	}
	
}
function bresumeClick(){
	switch(timer.status){
		case "conf":
			match.matchid = getCurrentTimestamp();
		case "ready":
			//start next period
			singleBeep();
			timer.status = "running";
			timer.period++;
			timer.start = getCurrentTimestamp();
			logEvent("Start " + getPeriodName(), getKickoffTeam(), null);
			updateScore();
			break;
		case "timeoff":
			//resume running
			singleBeep();
			timer.status = "running";
			timer.start += (getCurrentTimestamp() - timer.start_timeoff);
			logEvent("Resume time", null, null);
			break;
		case "rest":
			//get ready for next period
			timer.status = "ready";
			break;
	}
	updateButtons();
}
function brestClick(){
	logEvent("Result " + getPeriodName() + " " + match.home.tot + ":" + match.away.tot, null, null);

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
	updateButtons();

	if(match.events[match.events.length-1].what === "Time off"){
		match.events.splice(match.events.length-1, 1);
	}
	var kickoffTeam = getKickoffTeam();
	if(kickoffTeam !== null){
		$('#score_' + kickoffTeam.id).html(kickoffTeam.tot + " KICK");
	}
}
function bfinishClick(){
	timer.status = "finished";
	updateButtons();
	updateScore();

	if(match.events[match.events.length-1].what === "Rest start"){
		match.events.splice(match.events.length-1, 1);
	}
	file_storeMatch(match);
}
function bclearClick(){
	timer = {status:"conf",timer:0,start:0,start_timeoff:0,periodended:false,period:0};
	updateTimer();
	match.home = {id:"home",team:"home",color:"green",tot:0,tries:0,cons:0,goals:0,sinbins:[],kickoff:0};
	match.away = {id:"away",team:"away",color:"red",tot:0,tries:0,cons:0,goals:0,sinbins:[],kickoff:0};
	match.events = [];
	match.matchid = 0;
	updateScore();
	updateButtons();
	$('#sinbins_home').html("");
	$('#sinbins_away').html("");
	$('#home').css('background', match.home.color);
	$('#away').css('background', match.away.color);
	$('#matchSettings').show();
	$('#bconf').show();
}

function updateButtons(){
	var timerstatus = "";
	$('.bottombutton, .overtimerbutton, #bconfwatchimg').each(function (){$(this).hide();});
	switch(timer.status){
		case "conf":
			$('#bconf').show();
		case "ready":
			$('#bstart').show();
			break;
		case "timeoff":
			$('#bresume').show();
			$('#brest').show();
			$('#bconfwatchimg').show();
			timerstatus = "time off";
			break;
		case "rest":
			$('#bnext').show();
			$('#bfinish').show();
			$('#bconfwatchimg').show();
			timerstatus = "rest";
			break;
		case "finished":
			$('#breport').show();
			$('#bclear').show();
			timerstatus = "finished";
			break;
	}
	$('#timerstatus').html(timerstatus);
	updateTimer();
}

function update(){
	var millisecs = updateTime();
	switch(timer.status){
		case "running":
			updateSinbins();
		case "rest":
			updateTimer();
			break;
	}
	setTimeout(update, 1000 - millisecs);
}

function updateTime(){
	var d = new Date();
	var hours = d.getHours();
	var mins = d.getMinutes();
	var secs = d.getSeconds();
	var millisecs = d.getMilliseconds();

	var result = "";
	if(hours < 10){result += "0";}
	result += hours + ":";
	if(mins < 10){result += "0";}
	result += mins + ":";
	if(secs < 10){result += "0";}
	result += secs;

	$('#time').html(result);
	return millisecs;
}
function updateTimer(){
	var millisecs = 0;
	if(timer.status === "running" || timer.status === "rest"){
		millisecs = getCurrentTimestamp() - timer.start;
	}
	if(timer.status === "timeoff"){
		millisecs = timer.start_timeoff - timer.start;
	}
	timer.timer = millisecs;

	var temp = "";
	if(match.settings.timer_type === 1 && timer.status !== "rest"){
		millisecs = (match.settings.period_time * 60000) - millisecs;
	}
	if(millisecs < 0){
		millisecs -= millisecs * 2;
		temp = "-";
	}

	$('#timer').html(temp + prettyTimer(millisecs));

	if(!timer.periodended && timer.status === "running" && timer.timer > match.settings.period_time * 60000){
		timer.periodended = true;
		$('#timer').css('color', "red");
		beep();
	}
}
function prettyTimer(millisecs){
	var sec = Math.floor(millisecs / 1000);
	var mins = Math.floor(sec / 60);
	sec = sec % 60;

	var pretty = sec;
	if(sec < 10){pretty = "0" + pretty;}
	pretty = mins + ":" + pretty;

	return pretty;
}

function updateSinbins(){
	$('#sinbins_home').html(getSinbins(match.home.sinbins));
	$('#sinbins_away').html(getSinbins(match.away.sinbins));
}
function getSinbins(sinbins){
	var html = "";
	$.each(sinbins, function(index, sinbin){
		var remaining = sinbin.end - timer.timer;
		if(sinbin.hide === true || remaining < -(match.settings.sinbin / 2 * 60000)){
			sinbin.hide = true;
		}else if(sinbin.ended === true || remaining < 0){
			if(sinbin.ended !== true){
				beep();
				sinbin.ended = true;
			}
			html += '<span class="redtext">' + prettyTimer(0) + "</span><br>";
		}else{
			html += prettyTimer(remaining) + "<br>";
		}
	});
	return html;
}

function updateBattery(battery){
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
	if(timer.status === "conf"){
		if(match.home.kickoff === 1){
			match.home.kickoff = 0;
			$('#score_home').html(match.home.tot);
		}else{
			match.home.kickoff = 1;
			$('#score_home').html(match.home.tot + " KICK");
		}
		match.away.kickoff = 0;
		$('#score_away').html(match.away.tot);
		return;
	}
	team_edit = match.home;
	score_show();
}
function score_awayClick(){
	if(timer.status === "conf"){
		if(match.away.kickoff === 1){
			match.away.kickoff = 0;
			$('#score_away').html(match.away.tot);
		}else{
			match.away.kickoff = 1;
			$('#score_away').html(match.away.tot + " KICK");
		}
		match.home.kickoff = 0;
		$('#score_home').html(match.home.tot);
		return;
	}
	team_edit = match.away;
	score_show();
}
function score_show(){
	$('#score_player').val(0);
	$('#try').html(team_edit.tries);
	$('#con').html(team_edit.cons);
	$('#goal').html(team_edit.goals);

	$('#score_try').css('display', match.settings.points_try === 0 ? 'none' : 'block');
	$('#score_con').css('display', match.settings.points_con === 0 ? 'none' : 'block');
	$('#score_goal').css('display', match.settings.points_goal === 0 ? 'none' : 'block');

	if(match.settings.record_player === 1){
		$('#score_player_wrap').show();
	}else{
		$('#score_player_wrap').hide();
	}
	$('#score').show();
}
function tryClick(){
	team_edit.tries++;
	updateScore();
	$('#score').hide();
	var player = match.settings.record_player === 1 ? $('#score_player').val() : null;
	logEvent("TRY", team_edit, player);
}
function conversionClick(){
	team_edit.cons++;
	updateScore();
	$('#score').hide();
	var player = match.settings.record_player === 1 ? $('#score_player').val() : null;
	logEvent("CONVERSION", team_edit, player);
}
function goalClick(){
	team_edit.goals++;
	updateScore();
	$('#score').hide();
	var player = match.settings.record_player === 1 ? $('#score_player').val() : null;
	logEvent("GOAL", team_edit, player);
}
function updateScore(){
	match.home.tot = match.home.tries*match.settings.points_try + 
					match.home.cons*match.settings.points_con + 
					match.home.goals*match.settings.points_goal;
	$('#score_home').html(match.home.tot);
	match.away.tot = match.away.tries*match.settings.points_try + 
					match.away.cons*match.settings.points_con + 
					match.away.goals*match.settings.points_goal;
	$('#score_away').html(match.away.tot);
}
function cardsClick(){
	$('#card_player').val($('#score_player').val());
	$('#card').show();
	$('#score').hide();
}

function card_yellowClick(){
	$('#card').hide();
	var id = logEvent("YELLOW CARD", team_edit, $('#card_player').val());

	var end = timer.timer + (match.settings.sinbin*60000);
	end = Math.ceil(end/1000)*1000;
	team_edit.sinbins.push(JSON.parse('{"end":' + end + ',"ended":false,"hide":false,"id":' + id + '}'));

	updateSinbins();
}

function card_redClick(){
	$('#card').hide();
	logEvent("RED CARD", team_edit, $('#card_player').val());
}

function correctShow(){
	var items = "";
	var item = "";
	$.each(match.events, function(index, value){
		if(value.what !== "TRY" &&
			value.what !== "CONVERSION" &&
			value.what !== "GOAL" &&
			value.what !== "YELLOW CARD" &&
			value.what !== "RED CARD" 
		){
			return;
		}

		item = '<span onclick="removeEvent(\'' + index + '\')">';
		item += value.timer;
		item += ' ' + value.what;
		if(value.team){
			item += ' ' + getTeamName(value.team);
		}
		if(value.who){
			item += ' ' + value.who;
		}
		item += '</span><br>';
		items = item + items;
	});

	$('#correct').html(items);
	$('#correct_overlay').show();
}
function removeEvent(index){
	team_edit = match.events[index].team === match.home.id ? match.home : match.away;
	switch(match.events[index].what){
		case "TRY":
			team_edit.tries--;
			break;
		case "CONVERSION":
			team_edit.cons--;
			break;
		case "GOAL":
			team_edit.goals--;
			break;
		case "YELLOW CARD":
			var id = match.events[index].id;
			$.each(team_edit.sinbins, function (index, value){
				if(value.id === id){
					team_edit.sinbins.splice(index, 1);
				}
			});
			break;
		case "RED CARD":
			break;
	}
	match.events.splice(index, 1);

	updateScore();
	$('#correct_overlay').hide();
}
function bconfwatchClick(){
	if(timer.status === "conf" || timer.status === "running"){return;}
	$('#matchSettings').hide();
	bconfClick();
}
function bconfClick(){
	$('#color_home').css('background', match.home.color);
	$('#color_home').val(match.home.color);
	$('#color_away').css('background', match.away.color);
	$('#color_away').val(match.away.color);

	$('#match_type').val(match.settings.match_type);
	$('#period_time').val(match.settings.period_time);
	$('#period_count').val(match.settings.period_count);
	$('#sinbin').val(match.settings.sinbin);

	$('#points_try').val(match.settings.points_try);
	$('#points_con').val(match.settings.points_con);
	$('#points_goal').val(match.settings.points_goal);

	$('#record_player').prop('checked', match.settings.record_player === 1 ? true : false);
	$('#screen_on').prop('checked', match.settings.screen_on === 1 ? true : false);
	$('#timer_type').val(match.settings.timer_type);

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
	$('#custom_match').hide();
	match.settings.match_type = $('#match_type').val();
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
		case "custom":
			$('#custom_match').show();
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
	updateTimer();
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
function record_playerChange(){
	match.settings.record_player = $('#record_player').is(":checked") ? 1: 0;
	record_playerChanged();
}
function screen_onChange(){
	match.settings.screen_on = $('#screen_on').is(":checked") ? 1: 0;
	screen_onChanged();
}
function record_playerChanged(){
	if(match.settings.record_player === 1){
		$('#score').css('font-size', '15vh');
		$('#score_player_wrap').show();
		$('#card_yellow, #card_red').css('height', '');
		$('#card_player_wrap').show();
	}else{
		$('#score').css('font-size', '17vh');
		$('#score_player_wrap').hide();
		$('#card_yellow, #card_red').css('height', '40vh');
		$('#card_player_wrap').hide();
	}
}
function screen_onChanged(){
	if(match.settings.screen_on === 1){
		requestScreen_on();
	}else{
		tizen.power.release("SCREEN");
	}
}
function requestScreen_on(){
	if(match.settings.screen_on === 1){
		tizen.power.request("SCREEN", "SCREEN_NORMAL");
	}
}
function timer_typeChange(){
	match.settings.timer_type = parseInt($('#timer_type').val());
	timer_typeChanged();
}
function timer_typeChanged(){
	updateTimer();
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

function getKickoffTeam(){
	if(match.home.kickoff === 1){
		if(timer.period % 2 === 0){
			return match.home;
		}else{
			return match.away;
		}
	}
	if(match.away.kickoff === 1){
		if(timer.period % 2 === 0){
			return match.away;
		}else{
			return match.home;
		}
	}
	return null;
}

function getTeamName(id){
	return match.home.id === id ? match.home.team : match.away.team;
}

function logEvent(what, team, who){
	var id = Date.now();
	var currenttimer = timer.timer + ((timer.period-1)*match.settings.period_time*60000);
	var temp = '{"id":' + id +
				',"time":"' + $('#time').html() + '"' +
				',"timer":"' + prettyTimer(currenttimer) + '"' +
				',"what":"' + what + '"';
	if(team !== null){
		temp +=	',"team":"' + team.id + '"';
		if(who !== null && who !== "0"){
			temp +=	',"who":"' + who + '"';
		}
	}
	temp += '}';

	if(match.events.length > 1 && 
			what !== "Resume time" && 
			match.events[match.events.length-1].what === "Time off"
	){
		match.events.splice(match.events.length-1, 1);
	}

	console.log(temp);
	match.events.push(JSON.parse(temp));
	return id;
}

function showReport(){
	var html = "";
	$.each(match.events, function(index, value){
		if(value.what === "Resume time" ||
			value.what === "Time off"
		){
			return;
		}
		html += value.timer;
		html += " " + value.what;
		if(value.team){
			html += " " + getTeamName(value.team);
		}
		if(value.who){
			html += " " + value.who;
		}
		html += "<br>";
	});

	$('#report').html(html);
	$('#report_overlay').show();
}

function incomingSettings(newsettings){
	if(timer.status !== "conf"){
		console.log("not ready for settings");
		return false;
	}

	match.home.team = newsettings.home_name;
	match.home.color = newsettings.home_color;
	$('#home').css('background', match.home.color);
	match.away.team = newsettings.away_name;
	match.away.color = newsettings.away_color;
	$('#away').css('background', match.away.color);
	setNewSettings(newsettings);

	updateTimer();

	file_storeSettings(match.settings);
	return true;
}

function getSettings(){
	var ret = {};
	ret["home_name"] = match.home.team;
	ret["home_color"] = match.home.color;
	ret["away_name"] = match.away.team;
	ret["away_color"] = match.away.color;
	ret["match_type"] = match.settings.match_type;
	ret["period_time"] = match.settings.period_time;
	ret["period_count"] = match.settings.period_count;
	ret["sinbin"] = match.settings.sinbin;
	ret["points_try"] = match.settings.points_try;
	ret["points_con"] = match.settings.points_con;
	ret["points_goal"] = match.settings.points_goal;
	ret["record_player"] = match.settings.record_player;
	ret["screen_on"] = match.settings.screen_on;
	ret["timer_type"] = match.settings.timer_type;
	return ret;
}

function settingsRead(newsettings){
	if(timer.status !== "conf"){
		console.log("Ignore stored settings, game already started");
		return;
	}
	setNewSettings(newsettings);
}

function setNewSettings(newsettings){
	match.settings.match_type = newsettings.match_type;
	match.settings.period_time = newsettings.period_time;
	match.settings.period_count = newsettings.period_count;
	match.settings.sinbin = newsettings.sinbin;
	match.settings.points_try = newsettings.points_try;
	match.settings.points_con = newsettings.points_con;
	match.settings.points_goal = newsettings.points_goal;
	if(newsettings.hasOwnProperty('record_player')){
		match.settings.record_player = newsettings.record_player;
		record_playerChanged();
	}
	if(newsettings.hasOwnProperty('screen_on')){
		match.settings.screen_on = newsettings.screen_on;
		screen_onChanged();
	}
	if(newsettings.hasOwnProperty('countdown')){///DEPRECATED
		match.settings.timer_type = newsettings.countdown;
		timer_typeChanged();
	}
	if(newsettings.hasOwnProperty('timer_type')){
		match.settings.timer_type = newsettings.timer_type;
		timer_typeChanged();
	}
}
function beep(){
	console.log("beep beep");
	navigator.vibrate([500, 500, 500]);
}
function singleBeep(){
	console.log("beep");
	navigator.vibrate([300]);
}

function showMessage(message){
	console.log(message);
	$('#message').append(message);
	$('#message').show();
}
