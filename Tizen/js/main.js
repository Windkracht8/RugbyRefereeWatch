/* global $, file_init, file_storeMatch, file_storeSettings, file_storeCustomMatchTypes */
/* exported timerClick, bresumeClick, brestClick, bfinishClick, bclearClick, bconfwatchClick, score_homeClick, score_awayClick, tryClick, conversionClick, goalClick, foulplayClick, card_yellowClick, penalty_tryClick, card_redClick, bconfClick, color_homeChange, color_awayChange, match_typeChange, incomingSettings, getSettings, settingsRead, addCustomMatchType, syncCustomMatchTypes, customMatchTypesRead, removeEvent, record_playerChange, screen_onChange, timer_typeChange, showReport, showMessage */

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
		timer_type: 1,//0:up, 1:down
		help_version: 2
	},
	home: {
		id: "home",
		team: "home",
		color: "green",
		tot: 0,
		tries: 0,
		cons: 0,
		pen_tries: 0,
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
		pen_tries: 0,
		goals: 0,
		sinbins: [],
		kickoff: 0
	},
	events: [],
	matchid: 0
};
var custom_match_types = [];

window.onload = function(){
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
	if(tizen.systeminfo.getCapabilities().platformVersion.charAt(0) < 3){
		$('#stylesheet').attr("href", "css/style.2.css");
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
				showCorrect();
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
			timer.start = getCurrentTimestamp();
			var kickoffTeam = getKickoffTeam();//capture before increasing timer_period
			timer.period++;
			logEvent("Start " + getPeriodName(), kickoffTeam, null);
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
	if(match.events[match.events.length-1].what === "Time off"){
		match.events.splice(match.events.length-1, 1);
	}
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

	var kickoffTeam = getKickoffTeam();
	if(kickoffTeam !== null){
		$('#score_' + kickoffTeam.id).html(kickoffTeam.tot + " KICK");
	}
}
function bfinishClick(){
	timer.status = "finished";
	updateButtons();
	updateScore();

	file_storeMatch(match);
}
function bclearClick(){
	timer = {status:"conf",timer:0,start:0,start_timeoff:0,periodended:false,period:0};
	updateTimer();
	match.home = {id:"home",team:"home",color:"green",tot:0,tries:0,cons:0,pen_tries:0,goals:0,sinbins:[],kickoff:0};
	match.away = {id:"away",team:"away",color:"red",tot:0,tries:0,cons:0,pen_tries:0,goals:0,sinbins:[],kickoff:0};
	match.events = [];
	match.matchid = 0;
	updateScore();
	updateButtons();
	$('#sinbins_home').html("");
	$('#sinbins_away').html("");
	$('#home').css('background', checkColor(match.home.color));
	$('#away').css('background', checkColor(match.away.color));
	$('#matchSettings').show();
	$('#helpSettings').show();
	$('#bconf').show();
}

function updateButtons(){
	var timerstatus = "";
	$('.bottombutton, .overtimerbutton, #bconfwatch').each(function (){$(this).hide();});
	switch(timer.status){
		case "conf":
			$('#bconf').show();
		case "ready":
			$('#bstart').show();
			break;
		case "timeoff":
			$('#bresume').show();
			$('#brest').show();
			$('#bconfwatch').show();
			timerstatus = "time off";
			break;
		case "rest":
			$('#bnext').show();
			$('#bfinish').show();
			$('#bconfwatch').show();
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
	showScore();
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
	showScore();
}
function showScore(){
	$('#score_player').val(0);

	var padding = 0;
	if(match.settings.points_con === 0){
		$('#score_con').css('display', 'none');
		$('#penalty_try').css('display', 'none');
		padding += 4;
	}else{
		$('#score_con').css('display', 'block');
		$('#penalty_try').css('display', 'block');
	}
	if(match.settings.points_goal === 0){
		$('#score_goal').css('display', 'none');
		padding += 2;
	}else{
		$('#score_goal').css('display', 'block');
	}
	if(match.settings.record_player === 1){
		$('#score_player_wrap').show();
	}else{
		$('#score_player_wrap').hide();
		padding += 2;
	}

	$('.score').css('padding-bottom', padding + 'vh');
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
					match.home.pen_tries*(match.settings.points_try + match.settings.points_con) +
					match.home.goals*match.settings.points_goal;
	$('#score_home').html(match.home.tot);
	match.away.tot = match.away.tries*match.settings.points_try +
					match.away.cons*match.settings.points_con +
					match.away.pen_tries*(match.settings.points_try + match.settings.points_con) +
					match.away.goals*match.settings.points_goal;
	$('#score_away').html(match.away.tot);
}
function foulplayClick(){
	$('#foulplay_player').val($('#score_player').val());
	$('#foulplay').show();
	$('#score').hide();
}

function card_yellowClick(){
	$('#foulplay').hide();
	var id = logEvent("YELLOW CARD", team_edit, $('#foulplay_player').val());

	var end = timer.timer + (match.settings.sinbin*60000);
	end = Math.ceil(end/1000)*1000;
	team_edit.sinbins.push(JSON.parse('{"end":' + end + ',"ended":false,"hide":false,"id":' + id + '}'));

	updateSinbins();
}

function penalty_tryClick(){
	team_edit.pen_tries++;
	updateScore();
	$('#foulplay').hide();
	var player = match.settings.record_player === 1 ? $('#foulplay_player').val() : null;
	logEvent("PENALTY TRY", team_edit, player);
}

function card_redClick(){
	$('#foulplay').hide();
	logEvent("RED CARD", team_edit, $('#foulplay_player').val());
}

function showCorrect(){
	var items = "";
	var item = "";
	$.each(match.events, function(index, value){
		if(value.what !== "TRY" &&
			value.what !== "CONVERSION" &&
			value.what !== "PENALTY TRY" &&
			value.what !== "GOAL" &&
			value.what !== "YELLOW CARD" &&
			value.what !== "RED CARD" 
		){
			return;
		}

		item = '<div onclick="removeEvent(\'' + index + '\')">';
		item += value.timer;
		item += ' ' + value.what;
		if(value.team){
			item += ' ' + getTeamName(value.team);
		}
		if(value.who){
			item += ' ' + value.who;
		}
		item += '</div>';
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
		case "PENALTY TRY":
			team_edit.pen_tries--;
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
			updateSinbins();
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
	$('#helpSettings').hide();
	bconfClick();
}
function bconfClick(){
	$('#color_home').css('background', checkColor(match.home.color));
	$('#color_home').val(match.home.color);
	$('#color_away').css('background', checkColor(match.away.color));
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
	$('#conf').scrollTop(0);
}
function color_homeChange(){
	match.home.color = $('#color_home').val();
	$('#color_home').css('background', checkColor(match.home.color));
	$('#home').css('background', checkColor(match.home.color));
}
function color_awayChange(){
	match.away.color = $('#color_away').val();
	$('#color_away').css('background', checkColor(match.away.color));
	$('#away').css('background', checkColor(match.away.color));
}
function checkColor(color){
	switch(color){
		case "brown":
			return "#623412";
		case "orange":
			return "#FE5000";
		case "white":
			return "#D3D3D3";
		default:
			return color;
	}
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
	}else{
		$('#score').css('font-size', '17vh');
		$('#score_player_wrap').hide();
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
	$('#home').css('background', checkColor(match.home.color));
	match.away.team = newsettings.away_name;
	match.away.color = newsettings.away_color;
	$('#away').css('background', checkColor(match.away.color));
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
	if(!newsettings.hasOwnProperty('help_version')){
		noStoredSettings();
	}else if(newsettings.help_version !== match.settings.help_version){
		showHelp();
		file_storeSettings(match.settings);
	}
}

function noStoredSettings(){
	$('#help_welcome').show();
	showHelp();
	file_storeSettings(match.settings);
}

function addCustomMatchType(match_type){
	custom_match_types.push(match_type);
	loadCustomMatchTypesSelect();
	file_storeCustomMatchTypes();
}
function syncCustomMatchTypes(requestData){
	if(typeof(requestData) === "undefined" || typeof(requestData.custom_match_types) === "undefined"){return;}

	var hasupdate = false;

	custom_match_types = custom_match_types.filter(function(match_type){
		for(var p=0; p<requestData.custom_match_types.length; p++){
			if(match_type.name === requestData.custom_match_types[p].name){
				return true;
			}
		}
		hasupdate = true;
		return false;
    });

	requestData.custom_match_types.forEach(function(match_type){
		var found = false;
		for(var l=0; l<custom_match_types.length; l++){
			if(match_type.name === custom_match_types[l].name){
				found = true;
				break;
			}
		}
		if(!found){
			hasupdate = true;
			custom_match_types.push(match_type);
		}
	});

	if(hasupdate){
		loadCustomMatchTypesSelect();
		file_storeCustomMatchTypes();
	}
}
function customMatchTypesRead(newcustom_match_types){
	custom_match_types = newcustom_match_types;
	loadCustomMatchTypesSelect();
}
function loadCustomMatchTypesSelect(){
	for(var i=$("#match_type option").length-1; i>=6; i--){
		$("#match_type option")[i].remove();
	}
	custom_match_types.forEach(function(match_type){$("#match_type").append("<option>" + match_type.name + "</option>");});
}

function showHelp(){
	$('#help').show();
	$('#help').scrollTop(0);
	$('#conf').hide();
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
