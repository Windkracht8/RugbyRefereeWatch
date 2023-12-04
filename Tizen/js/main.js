/* global $, file_init, file_storeMatch, file_storeSettings, file_storeCustomMatchTypes, comms_start, comms_stop */
/* exported timerClick, bovertimerClick, bbottomClick, bconfwatchClick, extratimeChange, pen_homeClick, pen_awayClick, score_homeClick, score_awayClick, tryClick, conversionClick, goalClick, foulplayClick, card_yellowClick, penalty_tryClick, card_redClick, bconfClick, color_homeChange, color_awayChange, match_typeChange, incomingSettings, getSettings, settingsRead, addCustomMatchType, syncCustomMatchTypes, customMatchTypesRead, removeEvent, record_playerChange, screen_onChange, timer_typeClick, record_pensChange, showMessage, bluetoothChange */

var timer = {
	status: "conf",
	timer: 0,
	start: 0,
	start_timeoff: 0,
	period_ended: false,
	period: 0,
	period_time: 40,
	type: 1//0:up, 1:down
};
var settings = {
	screen_on: true,
	timer_type: 1,//0:up, 1:down
	record_player: false,
	record_pens: false,
	bluetooth: true,
	help_version: 4
};
var match = {
	settings: {
		match_type: "15s",
		period_time: 40,
		period_count: 2,
		sinbin: 10,
		points_try: 5,
		points_con: 2,
		points_goal: 3
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
		pens: 0,
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
		pens: 0,
		sinbins: [],
		kickoff: 0
	},
	events: [],
	matchid: 0,
	format: 1//September 2023
};
var custom_match_types = [];

window.onload = function(){
	if(typeof tizen === "undefined"){
		$('#test_back').show();
		$('body').css('border', 'solid thin white');
		$('body').css('border-radius', '50vh');
		settings.bluetooth = false;
	}
	document.addEventListener('tizenhwkey', function(e){
		if(e.keyName === "back"){
			back();
		}
	});
	document.addEventListener("rotarydetent", function(e){
		var scrollElement = null;
		if($('#help').is(":visible")){
			scrollElement = $('#help');
		}else if($('#conf').is(":visible")){
			scrollElement = $('#conf');
		}else if($('#correct_overlay').is(":visible")){
			scrollElement = $('#correct_overlay');
		}else if($('#report_overlay').is(":visible")){
			scrollElement = $('#report_overlay');
		}else{
			return;
		}
		if(e.detail.direction === "CW"){
			scrollElement.scrollTop(scrollElement.scrollTop() + 50);
		}else{
			scrollElement.scrollTop(scrollElement.scrollTop() - 50);
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
	record_pensChanged();

	function onScreenStateChanged(previousState, newState){
		if(newState === 'SCREEN_NORMAL' && settings.screen_on){
			tizen.power.request("SCREEN", "SCREEN_NORMAL");
		}
	}
	try{
		tizen.power.setScreenStateChangeListener(onScreenStateChanged);
	}catch(e){
		console.log("setScreenStateChangeListener exception " + e.message);
	}
	try{
		if(tizen.systeminfo.getCapability("http://tizen.org/feature/platform.core.api.version").charAt(0) < 3){
			$('#stylesheet').attr("href", "css/style.2.css");
		}
	}catch(e){
		console.log("getCapability exception " + e.message);
	}
	setTimeout(hideSplash, 1000);
};
 
function back(){
	try {
		var backdone = false;
		$('.overlay').each(function(){
			if($(this).is(":visible")){
				$(this).hide();
				if($(this).attr("id") === "conf"){
					storeSettings();
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

var timeOffBuzzTimeoutID;
function timerClick(){
	if(timer.status === "running"){
		//time off
		singleBeep();
		timer.status = "timeoff";
		timer.start_timeoff = getCurrentTimestamp();
		logEvent("Time off", null, null);
		updateButtons();
		timeOffBuzzTimeoutID = setTimeout(timeOffBuzz, 15000);
	}
}

function timeOffBuzz(){
	if(timer.status === "timeoff"){
		beep();
		timeOffBuzzTimeoutID = setTimeout(timeOffBuzz, 15000);
	}
}

function bovertimerClick(){
	if(timer.status === "conf"){
		match.matchid = getCurrentTimestamp();
	}
	switch(timer.status){
		case "conf":
		case "ready":
			//start next period
			singleBeep();
			timer.status = "running";
			timer.start = getCurrentTimestamp();
			var kickoffTeam = getKickoffTeam();//capture before increasing timer_period
			timer.period++;
			logEvent("START", kickoffTeam, null);
			updateScore();
			comms_stop();
			break;
		case "timeoff":
			//resume running
			clearTimeout(timeOffBuzzTimeoutID);
			singleBeep();
			timer.status = "running";
			timer.start += (getCurrentTimestamp() - timer.start_timeoff);
			logEvent("Resume time", null, null);
			break;
		case "rest":
			//get ready for next period
			timer.status = "ready";
			timer.type = settings.timer_type;
			break;
		case "finished":
			showReport();
			break;
		default://ignore
			return;
	}
	updateButtons();
}

function bbottomClick(){
	switch(timer.status){
		case "conf":
			showConf();
			break;
		case "timeoff":
			if(match.events[match.events.length-1].what === "Time off"){
				match.events.splice(match.events.length-1, 1);
			}
			logEvent("END", null, null);

			timer.status = "rest";
			timer.start = getCurrentTimestamp();
			timer.period_ended = false;
			timer.type = 0;
			$('#timer').css('color', "unset");
			$.each(match.home.sinbins, function(index, value){
				value.end = value.end - timer.timer;
			});
			$.each(match.away.sinbins, function(index, value){
				value.end = value.end - timer.timer;
			});

			var kickoffTeam = getKickoffTeam();
			if(kickoffTeam !== null){
				$('#score_' + kickoffTeam.id).html(kickoffTeam.tot + " KICK");
			}
			break;
		case "rest":
			timer.status = "finished";
			timer.type = settings.timer_type;
			updateScore();
			$('#conf, #timer_type').css("font-size", "unset");

			file_storeMatch(match);
			if(settings.bluetooth){
				comms_start();
			}
			break;
		case "finished":
			timer = {status:"conf",timer:0,start:0,start_timeoff:0,period_ended:false,period:0,period_time:match.settings.period_time,type:settings.timer_type};
			updateTimer();
			match.home = {id:"home",team:"home",color:match.home.color,tot:0,tries:0,cons:0,pen_tries:0,goals:0,pens:0,sinbins:[],kickoff:0};
			match.away = {id:"away",team:"away",color:match.away.color,tot:0,tries:0,cons:0,pen_tries:0,goals:0,pens:0,sinbins:[],kickoff:0};
			match.events = [];
			match.matchid = 0;
			updateScore();
			$('#sinbins_home').html("");
			$('#sinbins_away').html("");
			$('#matchSettings').show();
			$('#otherSettings').show();
			$('#bconf').show();
			break;
		default://ignore
			return;
	}
	updateButtons();
}

function updateButtons(){
	switch(timer.status){
		case "conf":
			$('#bovertimer').html('start');
			$('#bovertimer').show();
			$('#bbottom').html('<img src="res/gear.png">');
			$('#bbottom').show();
			$('#bconfwatch').hide();
			$('#extratime').hide();
			break;
		case "ready":
			if(timer.period >= match.settings.period_count){
				$('#extratime').show();
				extratimeChange();
				if(timer.period === match.settings.period_count){
					$('#bovertimer').html('start extra time');
				}else{
					$('#bovertimer').html('start extra time ' + (timer.period-match.settings.period_count+1));
				}
			}else{
				$('#extratime').hide();
				switch(timer.period){
					case 1:
						$('#bovertimer').html('start 2nd');
						break;
					case 2:
						$('#bovertimer').html('start 3rd');
						break;
					default:
						$('#bovertimer').html('start ' + timer.period + "th");
						break;
				}
			}
			$('#bovertimer').show();
			$('#bbottom').hide();
			$('#bconfwatch').hide();
			break;
		case "timeoff":
			$('#bovertimer').html('resume');
			$('#bovertimer').show();
			if(match.settings.period_count === 2 && timer.period === 1){
				$('#bbottom').html('half time');
			}else if(match.settings.period_count === 2 && timer.period === 2){
					$('#bbottom').html('full time');
			}else if(timer.period > match.settings.period_count){
				if(timer.period === match.settings.period_count+1){
					$('#bbottom').html('end extra');
				}else{
					$('#bbottom').html('end extra ' + (timer.period-match.settings.period_count));
				}
			}else{
				switch(timer.period){
					case 1:
						$('#bbottom').html('end 1st');
						break;
					case 2:
						$('#bbottom').html('end 2nd');
						break;
					case 3:
						$('#bbottom').html('end 3rd');
						break;
					default:
						$('#bbottom').html('end ' + timer.period + "th");
						break;
				}
			}
			$('#bbottom').show();
			$('#bconfwatch').show();
			$('#extratime').hide();
			break;
		case "rest":
			if(match.settings.period_count === 2 && timer.period === 1){
				$('#bovertimer').html('2nd half');
			}else if(timer.period >= match.settings.period_count){
				if(timer.period === match.settings.period_count){
					$('#bovertimer').html('extra time');
				}else{
					$('#bovertimer').html('extra time ' + (timer.period-match.settings.period_count+1));
				}
			}else{
				if(timer.period === 2){
					$('#bovertimer').html('3rd period');
				}else{
					$('#bovertimer').html(timer.period + "th period");
				}
			}
			$('#bovertimer').show();
			$('#bbottom').html('finish');
			$('#bbottom').show();
			$('#bconfwatch').show();
			$('#extratime').hide();
			break;
		case "finished":
			$('#bovertimer').html('report');
			$('#bovertimer').show();
			$('#bbottom').html('clear');
			$('#bbottom').show();
			$('#bconfwatch').hide();
			$('#extratime').hide();
			break;
		default:
			$('#bovertimer').hide();
			$('#bbottom').hide();
			$('#bconfwatch').hide();
			$('#extratime').hide();
	}
	updateTimer();
}

function update(){
	var millisecs = updateTime();
	if(timer.status === "running"){
		updateSinbins();
		updateTimer();
	}else if(timer.status === "rest"){
		updateTimer();
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
	if(timer.type === 1){
		millisecs = (timer.period_time * 60000) - millisecs;
	}
	if(millisecs < 0){
		millisecs -= millisecs * 2;
		temp = "-";
	}

	$('#timer').html(temp + prettyTimer(millisecs));

	if(!timer.period_ended && timer.status === "running" && timer.timer > timer.period_time * 60000){
		timer.period_ended = true;
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

function pen_homeClick(){
	if(timer.status === "conf"){return;}
	match.home.pens++;
	updateScore();
	logEvent("PENALTY", match.home, null);
}
function pen_awayClick(){
	if(timer.status === "conf"){return;}
	match.away.pens++;
	updateScore();
	logEvent("PENALTY", match.away, null);
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
	if(settings.record_player){
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
	var player = settings.record_player ? $('#score_player').val() : null;
	logEvent("TRY", team_edit, player);
}
function conversionClick(){
	team_edit.cons++;
	updateScore();
	$('#score').hide();
	var player = settings.record_player ? $('#score_player').val() : null;
	logEvent("CONVERSION", team_edit, player);
}
function goalClick(){
	team_edit.goals++;
	updateScore();
	$('#score').hide();
	var player = settings.record_player ? $('#score_player').val() : null;
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

	$('#pen_home').html(match.home.pens);
	$('#pen_away').html(match.away.pens);
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
	var player = settings.record_player ? $('#foulplay_player').val() : null;
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
			value.what !== "PENALTY" &&
			value.what !== "YELLOW CARD" &&
			value.what !== "RED CARD" 
		){
			return;
		}

		item = '<div onclick="removeEvent(\'' + index + '\')">';
		item += prettyTimer(value.timer);
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
		case "PENALTY":
			team_edit.pens--;
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
	$('#conf, #timer_type').css("font-size", "8vh");
	$('#matchSettings').hide();
	$('#otherSettings').hide();
	showConf();
}
function showConf(){
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

	$('#screen_on').prop('checked', settings.screen_on);
	$('#timer_type').val(settings.timer_type);
	$('#record_player').prop('checked', settings.record_player);
	$('#record_pens').prop('checked', settings.record_pens);
	$('#bluetooth').prop('checked', settings.bluetooth);

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
		default:
			custom_match_types.forEach(function(match_type){
				if(match_type.name === $('#match_type').val()){
					$('#period_time').val(match_type.period_time);
					$('#period_count').val(match_type.period_count);
					$('#sinbin').val(match_type.sinbin);
					$('#points_try').val(match_type.points_try);
					$('#points_con').val(match_type.points_con);
					$('#points_goal').val(match_type.points_goal);
				}
			});
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
	timer.period_time = match.settings.period_time;
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
	settings.record_player = $('#record_player').is(":checked");
	record_playerChanged();
}
function record_playerChanged(){
	if(settings.record_player){
		$('#score').css('font-size', '15vh');
		$('#score_player_wrap').show();
	}else{
		$('#score').css('font-size', '17vh');
		$('#score_player_wrap').hide();
	}
}
function record_pensChange(){
	settings.record_pens = $('#record_pens').is(":checked");
	record_pensChanged();
}
function record_pensChanged(){
	$('#pen').css('display', settings.record_pens ? 'block' : 'none');
}
function screen_onChange(){
	settings.screen_on = $('#screen_on').is(":checked");
	screen_onChanged();
}
function screen_onChanged(){
	if(settings.screen_on){
		tizen.power.request("SCREEN", "SCREEN_NORMAL");
	}else{
		tizen.power.release("SCREEN");
	}
}
function bluetoothChange(){
	settings.bluetooth = $('#bluetooth').is(":checked");
	if(settings.bluetooth){
		comms_start();
	}else{
		comms_stop();
	}
}
function timer_typeClick(){
	settings.timer_type = settings.timer_type === 0 ? 1 : 0;
	timer.type = settings.timer_type;
	timer_typeChanged();
}
function timer_typeChanged(){
	if(settings.timer_type === 0){
		$('#timer_type_0').show();
		$('#timer_type_1').hide();
	}else{
		$('#timer_type_0').hide();
		$('#timer_type_1').show();
	}
	updateTimer();
}

function getPeriodName(period){
	if(period > match.settings.period_count){
		if(period === match.settings.period_count+1){
			return "extra time";
		}else{
			return "extra time " + (period-match.settings.period_count);
		}
	}
	
	if(match.settings.period_count === 2){
		switch(period){
			case 1:
				return "first half";
			case 2:
				return "second half";
		}
	}
	return "period " + period;
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
	var currenttimer = timer.timer + ((timer.period-1)*timer.period_time*60000);
	var temp = '{"id":' + id +
				',"time":"' + $('#time').html() + '"' +
				',"timer":"' + currenttimer + '"' +
				',"period":"' + timer.period + '"' +
				',"what":"' + what + '"';
	if(team !== null){
		temp +=	',"team":"' + team.id + '"';
		if(who !== null && who !== "0"){
			temp +=	',"who":"' + who + '"';
		}
	}
	if(what === "END"){
		temp += ',"score":"' + match.home.tot + ':' + match.away.tot + '"';
	}
	temp += '}';

	console.log(temp);
	match.events.push(JSON.parse(temp));
	return id;
}

function showReport(){
	var html = "";
	var period = 0;
	$.each(match.events, function(index, value){
		if(value.what === "Resume time" ||
			value.what === "Time off"
		){
			return;
		}
		var what = value.what;
		if(what === "START"){
			period++;
			what = "Start " + getPeriodName(period);
		}
		if(what === "END"){
			what = "Result " + getPeriodName(period) + " " + value.score;
		}
		html += prettyTimer(value.timer);
		html += " " + what;
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

	storeSettings();
	return true;
}

function getSettings(){
	var ret = {};
	ret.home_name = match.home.team;
	ret.home_color = match.home.color;
	ret.away_name = match.away.team;
	ret.away_color = match.away.color;
	ret.match_type = match.settings.match_type;
	ret.period_time = match.settings.period_time;
	ret.period_count = match.settings.period_count;
	ret.sinbin = match.settings.sinbin;
	ret.points_try = match.settings.points_try;
	ret.points_con = match.settings.points_con;
	ret.points_goal = match.settings.points_goal;
	ret.screen_on = settings.screen_on;
	ret.timer_type = settings.timer_type;
	ret.record_player = settings.record_player;
	ret.record_pens = settings.record_pens;
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
	}else if(newsettings.help_version !== settings.help_version){
		showHelp();
		storeSettings();
	}
	hideSplash();
	if(settings.bluetooth){
		comms_start();
	}else{
		comms_stop();
	}
}

function noStoredSettings(){
	$('#help_welcome').show();
	showHelp();
	comms_start();
	storeSettings();
}

function storeSettings(){
	var newsettings = JSON.parse(JSON.stringify(settings));
	newsettings.match_type = match.settings.match_type;
	newsettings.period_time = match.settings.period_time;
	newsettings.period_count = match.settings.period_count;
	newsettings.sinbin = match.settings.sinbin;
	newsettings.points_try = match.settings.points_try;
	newsettings.points_con = match.settings.points_con;
	newsettings.points_goal = match.settings.points_goal;
	newsettings.home_color = match.home.color;
	newsettings.away_color = match.away.color;
	file_storeSettings(newsettings);
}

function addCustomMatchType(match_type){
	custom_match_types.push(match_type);
	loadCustomMatchTypesSelect();
	file_storeCustomMatchTypes(custom_match_types);
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
		file_storeCustomMatchTypes(custom_match_types);
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
function checkMatchType(newsettings){
	for(var i=0; i<custom_match_types.length; i++){
		if(newsettings.match_type === custom_match_types[i].name){
			return;
		}
	}
	for(var j=0; j<$("#match_type option").length; j++){
		if(newsettings.match_type === $("#match_type option")[j].innerText){
			return;
		}
	}
	addCustomMatchType({
		"name": newsettings.match_type,
		"period_time": newsettings.period_time,
		"period_count": newsettings.period_count,
		"sinbin": newsettings.sinbin,
		"points_try": newsettings.points_try,
		"points_con": newsettings.points_con,
		"points_goal": newsettings.points_goal
	});
}

function showHelp(){
	$('#help').show();
	$('#help').scrollTop(0);
	$('#conf').hide();
	$('#help').trigger("focus");
}

function setNewSettings(newsettings){
	if(newsettings.hasOwnProperty('home_color')){
		$('#color_home').val(newsettings.home_color);
		color_homeChange();
	}
	if(newsettings.hasOwnProperty('away_color')){
		$('#color_away').val(newsettings.away_color);
		color_awayChange();
	}

	match.settings.match_type = newsettings.match_type;
	match.settings.period_time = newsettings.period_time;
	timer.period_time = match.settings.period_time;
	match.settings.period_count = newsettings.period_count;
	match.settings.sinbin = newsettings.sinbin;
	match.settings.points_try = newsettings.points_try;
	match.settings.points_con = newsettings.points_con;
	match.settings.points_goal = newsettings.points_goal;
	if(newsettings.hasOwnProperty('screen_on')){
		if(typeof newsettings.screen_on !== "boolean"){//DEPRECATED Sep 2023
			newsettings.screen_on = Boolean(newsettings.screen_on);
		}
		settings.screen_on = newsettings.screen_on;
		screen_onChanged();
	}
	if(newsettings.hasOwnProperty('timer_type')){
		settings.timer_type = newsettings.timer_type;
		timer.type = settings.timer_type;
		timer_typeChanged();
	}
	if(newsettings.hasOwnProperty('record_player')){
		if(typeof newsettings.record_player !== "boolean"){//DEPRECATED Sep 2023
			newsettings.record_player = Boolean(newsettings.record_player);
		}
		settings.record_player = newsettings.record_player;
		record_playerChanged();
	}
	if(newsettings.hasOwnProperty('record_pens')){
		if(typeof newsettings.record_pens !== "boolean"){//DEPRECATED Sep 2023
			newsettings.record_pens = Boolean(newsettings.record_pens);
		}
		settings.record_pens = newsettings.record_pens;
		record_pensChanged();
	}
	if(newsettings.hasOwnProperty('bluetooth')){
		settings.bluetooth = newsettings.bluetooth;
	}
	if(newsettings.hasOwnProperty('help_version')){
		settings.help_version = newsettings.help_version;
	}
	checkMatchType(newsettings);
}

function extratimeChange(){
	switch($('#extratime').val()){
		case "UP":
			timer.type = 0;
			timer.period_time = match.settings.period_time;
			break;
		default:
			timer.type = settings.timer_type;
			timer.period_time = parseInt($('#extratime').val());
	}
	updateTimer();
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

function hideSplash(){$('#splash').hide();}
