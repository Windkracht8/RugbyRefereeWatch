/*
 * Copyright 2020-2026 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.windkracht8.rugbyrefereewatch

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.windkracht8.rugbyrefereewatch.MatchData.Companion.HOME_ID
import com.windkracht8.rugbyrefereewatch.MatchData.Companion.AWAY_ID
import com.windkracht8.rugbyrefereewatch.MatchData.EventWhat
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class MatchData: Comparable<MatchData> {
	companion object {
		const val HOME_ID: String = "home"
		const val AWAY_ID: String = "away"
		const val FORMAT = 4 //October 2025; replacements added
	}

	enum class EventWhat {
		START, END, TRY, CONVERSION, PENALTY_TRY, GOAL, DROP_GOAL, PENALTY_GOAL,
		YELLOW_CARD, RED_CARD, PENALTY, TIME_OFF, RESUME, REPLACEMENT
	}

	var format: Int
	val matchId: Long
	val events: SnapshotStateList<Event> = mutableStateListOf()
	val home: Team
	val away: Team
	val matchType: String
	val periodTime: Int
	val periodCount: Int
	val sinbin: Int
	val pointsTry: Int
	val pointsCon: Int
	val pointsGoal: Int
	val clockPk: Int
	val clockCon: Int
	val clockRestart: Int

	var calcScoreHome: Int = 0
	var calcScoreAway: Int = 0

	constructor(matchJson: JSONObject, checkTeamNames: Boolean = false) {
		matchId = matchJson.optLong("matchid", 0)
		val format: Int = matchJson.optInt("format", 0)
		if (format > 4) runInBackground { error.emit(R.string.update_mobile_app) }
		if (format < 2) updateFormat1to2(matchJson)
		if (format < 3) updateFormat2to3(matchJson)
		this.format = FORMAT

		home = Team(matchJson.getJSONObjectOrEmpty(HOME_ID), checkTeamNames)
		away = Team(matchJson.getJSONObjectOrEmpty(AWAY_ID), checkTeamNames)

		val settings = matchJson.getJSONObjectOrEmpty("settings")
		matchType = settings.optString("match_type", "15s")
		periodTime = settings.optInt("period_time", 0)
		periodCount = settings.optInt("period_count", 0)
		sinbin = settings.optInt("sinbin", 0)
		pointsTry = settings.optInt("points_try", 0)
		pointsCon = settings.optInt("points_con", 0)
		pointsGoal = settings.optInt("points_goal", 0)
		clockPk = settings.optInt("clock_pk", 0)
		clockCon = settings.optInt("clock_con", 0)
		clockRestart = settings.optInt("clock_restart", 0)

		val eventsJson = matchJson.getJSONArrayOrEmpty("events")
		for (i in 0..<eventsJson.length()) {
			events.add(Event(eventsJson.getJSONObjectOrEmpty(i)))
		}
	}
	constructor(original: MatchData) {
		matchId = original.matchId
		format = original.format
		home = Team(original.home)
		away = Team(original.away)
		original.events.forEach { events.add(Event(it)) }

		matchType = original.matchType
		periodTime = original.periodTime
		periodCount = original.periodCount
		sinbin = original.sinbin
		pointsTry = original.pointsTry
		pointsCon = original.pointsCon
		pointsGoal = original.pointsGoal
		clockPk = original.clockPk
		clockCon = original.clockCon
		clockRestart = original.clockRestart
	}
	fun updateFormat1to2(match: JSONObject) {
		//Format 2; Dec 2024; added yellow/red card count
		var homeYellowCards = 0
		var awayYellowCards = 0
		var homeRedCards = 0
		var awayRedCards = 0
		val events = match.getJSONArray("events")
		for (i in 0..<events.length()) {
			val event = events.getJSONObject(i)
			if (event.getString("what") == "YELLOW CARD") {
				if (event.getString("team").isHome()) {
					homeYellowCards++
				} else {
					awayYellowCards++
				}
			}
			if (event.getString("what") == "RED CARD") {
				if (event.getString("team").isHome()) {
					homeRedCards++
				} else {
					awayRedCards++
				}
			}
		}
		val home = match.getJSONObject(HOME_ID)
		home.put("yellow_cards", homeYellowCards)
		home.put("red_cards", homeRedCards)
		match.put(HOME_ID, home)
		val away = match.getJSONObject(AWAY_ID)
		away.put("yellow_cards", awayYellowCards)
		away.put("red_cards", awayRedCards)
		match.put(AWAY_ID, away)
		match.put("format", 2)
	}
	fun updateFormat2to3(match: JSONObject) {
		//Format 3; April 2025; change timer from ms to s
		val events = match.getJSONArray("events")
		for (i in 0..<events.length()) {
			tryIgnore {
				val event = events.getJSONObject(i)
				val timer = event.getLong("timer")
				event.put("timer", Math.floorDiv(timer, 1000))
			}
		}
		match.put("format", 3)
	}
	fun recalc() {
		listOf(home, away).forEach { team ->
			team.tries = 0
			team.cons = 0
			team.penTries = 0
			team.goals = 0
			team.dropGoals = 0
			team.penGoals = 0
			team.yellowCards = 0
			team.redCards = 0
			team.pens = 0
		}
		events.forEach {
			val team = when (it.isHome) {
				true -> home
				false -> away
				null -> null
			}
			when(it.what) {
				EventWhat.TRY -> team?.tries++
				EventWhat.CONVERSION -> team?.cons++
				EventWhat.PENALTY_TRY -> team?.penTries++
				EventWhat.GOAL -> team?.goals++
				EventWhat.DROP_GOAL -> team?.dropGoals++
				EventWhat.PENALTY_GOAL -> team?.penGoals++
				EventWhat.YELLOW_CARD -> team?.yellowCards++
				EventWhat.RED_CARD -> team?.redCards++
				EventWhat.PENALTY -> team?.pens++
				else -> {}
			}
		}
		listOf(home, away).forEach { team ->
			team.tot = team.tries * pointsTry +
					team.cons * pointsCon +
					team.penTries * (pointsTry + pointsCon) +
					(team.goals + team.dropGoals + team.penGoals) * pointsGoal
		}
	}
	suspend fun toJson(): JSONObject {
		val ret = JSONObject()
		try {
			ret.put("matchid", matchId)
			ret.put("format", FORMAT)
			val settings = JSONObject().put("match_type", matchType)
				.put("period_time", periodTime)
				.put("period_count", periodCount)
				.put("sinbin", sinbin)
				.put("points_try", pointsTry)
				.put("points_con", pointsCon)
				.put("points_goal", pointsGoal)
				.put("clock_pk", clockPk)
				.put("clock_con", clockCon)
				.put("clock_restart", clockRestart)
			ret.put("settings", settings)
			ret.put(HOME_ID, home.toJson())
			ret.put(AWAY_ID, away.toJson())
			val eventsJson = JSONArray()
			for(event in events) eventsJson.put(event.toJson())
			ret.put("events", eventsJson)
		} catch (e: JSONException) {
			logE("MatchData.toJson Exception: " + e.message)
			error.emit(R.string.fail_save_matches)
		}
		//logD{"MatchData.toJson result: $ret"}
		return ret
	}
	override fun compareTo(other: MatchData): Int = matchId.compareTo(other.matchId)

	inner class Team {
		val id: String
		val color: String
		var team: String by mutableStateOf("")
		var tot: Int
		var tries: Int
		var cons: Int
		var penTries: Int
		var goals: Int
		var dropGoals: Int
		var penGoals: Int
		var yellowCards: Int
		var redCards: Int
		var pens: Int
		val kickoff: Boolean

		constructor(teamJson: JSONObject, checkName: Boolean = false) {
			id = teamJson.optString("id", HOME_ID)
			color = teamJson.optString("color", "green")
			tot = teamJson.optInt("tot", 0)
			tries = teamJson.optInt("tries", 0)
			cons = teamJson.optInt("cons", 0)
			penTries = teamJson.optInt("pen_tries", 0)
			goals = teamJson.optInt("goals", 0)
			dropGoals = teamJson.optInt("drop_goals", 0)
			penGoals = teamJson.optInt("pen_goals", 0)
			yellowCards = teamJson.optInt("yellow_cards", 0)
			redCards = teamJson.optInt("red_cards", 0)
			pens = teamJson.optInt("pens", 0)
			kickoff = teamJson.optBoolean("kickoff", true)

			var name = teamJson.optString("team", id)
			team = if(checkName && name.equals(id)) color else name
		}
		constructor(original: Team) {
			id = original.id
			color = original.color
			team = original.team
			tot = original.tot
			tries = original.tries
			cons = original.cons
			penTries = original.penTries
			goals = original.goals
			dropGoals = original.dropGoals
			penGoals = original.penGoals
			yellowCards = original.yellowCards
			redCards = original.redCards
			pens = original.pens
			kickoff = original.kickoff
		}
		suspend fun toJson(): JSONObject {
			val ret = JSONObject()
			try {
				ret.put("id", id)
				ret.put("team", team)
				ret.put("color", color)
				ret.put("tot", tot)
				ret.put("tries", tries)
				ret.put("cons", cons)
				ret.put("pen_tries", penTries)
				ret.put("goals", goals)
				ret.put("drop_goals", dropGoals)
				ret.put("pen_goals", penGoals)
				ret.put("yellow_cards", yellowCards)
				ret.put("red_cards", redCards)
				ret.put("pens", pens)
				ret.put("kickoff", kickoff)
			} catch (e: JSONException) {
				logE("MatchData.match.toJson Exception: " + e.message)
				error.emit(R.string.fail_save_matches)
			}
			return ret
		}
	}

	inner class Event {
		val id: Long
		val time: String
		val timer: Int
		var what: EventWhat by mutableStateOf(EventWhat.START)
		val period: Int
		var isHome: Boolean? by mutableStateOf(null)
		val teamName: String?
		var who: Int? by mutableStateOf(null)
		var whoEnter: Int? by mutableStateOf(null)
		var whoLeave: Int? by mutableStateOf(null)
		val score: String
		var reason: String? by mutableStateOf(null)

		constructor (eventJson: JSONObject) {
			id = eventJson.optLong("id", 0)
			time = eventJson.optString("time", "00:00:00")
			timer = eventJson.optInt("timer", 0)
			period = eventJson.optInt("period", 0)
			isHome = if(!eventJson.has("team")) null
					else eventJson.optString("team") == HOME_ID
			teamName = isHome?.let { if(it) home.team else away.team }
			who = eventJson.getIntOrNull("who")
			whoEnter = eventJson.getIntOrNull("who_enter")
			whoLeave = eventJson.getIntOrNull("who_leave")
			reason = eventJson.getStringOrNull("reason")
			when(eventJson.optString("what", "")){
				"TRY" -> {
					what = EventWhat.TRY
					if(isHome == true) calcScoreHome += pointsTry
					else calcScoreAway += pointsTry
				}
				"CONVERSION" -> {
					what = EventWhat.CONVERSION
					if(isHome == true) calcScoreHome += pointsCon
					else calcScoreAway += pointsCon
				}
				"PENALTY TRY" -> {
					what = EventWhat.PENALTY_TRY
					if(isHome == true) calcScoreHome += pointsTry + pointsCon
					else calcScoreAway += pointsTry + pointsCon
				}
				"GOAL" -> {
					what = EventWhat.GOAL
					if(isHome == true) calcScoreHome += pointsGoal
					else calcScoreAway += pointsGoal
				}
				"DROP GOAL" -> {
					what = EventWhat.DROP_GOAL
					if(isHome == true) calcScoreHome += pointsGoal
					else calcScoreAway += pointsGoal
				}
				"PENALTY GOAL" -> {
					what = EventWhat.PENALTY_GOAL
					if(isHome == true) calcScoreHome += pointsGoal
					else calcScoreAway += pointsGoal
				}
				"END" -> what = EventWhat.END
				"START" -> what = EventWhat.START
				"YELLOW CARD" -> what = EventWhat.YELLOW_CARD
				"RED CARD" -> what = EventWhat.RED_CARD
				"PENALTY" -> what = EventWhat.PENALTY
				"TIME OFF" -> what = EventWhat.TIME_OFF
				"RESUME" -> what = EventWhat.RESUME
				"REPLACEMENT" -> what = EventWhat.REPLACEMENT
				else -> what = EventWhat.START
			}
			score = eventJson.getStringOrNull("score") ?: "$calcScoreHome:$calcScoreAway"
		}
		constructor (original: Event) {
			id = original.id
			time = original.time
			timer = original.timer
			what = original.what
			period = original.period
			isHome = original.isHome
			teamName = original.teamName
			who = original.who
			whoEnter = original.whoEnter
			whoLeave = original.whoLeave
			score = original.score
			reason = original.reason
		}

		fun delete() = events.remove(this)

		fun prettyTimer(): String {
			val minutes = Math.floorDiv(timer, 60)
			var timer = minutes.toString()
			if (minutes > periodTime.toLong() * periodCount) {
				timer = (periodTime * periodCount).toString()
				val over = minutes - (periodTime.toLong() * periodCount)
				timer += "+$over"
			}
			return "$timer'"
		}
		fun prettyTimerFull(): String {
			val minutes = Math.floorDiv(timer, 60)
			val seconds = timer - minutes * 60
			var timer = minutes.toString()
			if (minutes > periodTime.toLong() * periodCount) {
				timer = (periodTime * periodCount).toString()
				val over = minutes - (periodTime.toLong() * periodCount)
				timer += "+$over"
			}
			return if (seconds < 10) "$timer'0$seconds" else "$timer'$seconds"
		}
		fun prettyPeriod(): String {
			val ret = if(what == EventWhat.START) "Start" else "Result"
			if (period > periodCount) {
				return if (period == periodCount + 1) {
					"$ret extra time"
				} else {
					"$ret extra time " + (period - periodCount)
				}
			} else if (periodCount == 2) {
				when (period) {
					1 -> return "$ret first half"
					2 -> return "$ret second half"
				}
			} else {
				when (period) {
					1 -> return "$ret 1st"
					2 -> return "$ret 2nd"
					3 -> return "$ret 3rd"
					4 -> return "$ret 4th"
				}
			}
			return "$ret period $period"
		}

		suspend fun toJson(): JSONObject {
			val ret = JSONObject()
			try {
				ret.put("id", id)
					.put("time", time)
					.put("timer", timer)
					.put("period", period)
					.put("what", what.name.replace("_", " "))
				if (isHome != null) {
					ret.put("team", if(isHome == true) HOME_ID else AWAY_ID)
					if (who != null) { ret.put("who", who) }
					if (whoEnter != null) { ret.put("who_enter", whoEnter) }
					if (whoLeave != null) { ret.put("who_leave", whoLeave) }
				}
				ret.put("score", score)
			} catch (e: JSONException) {
				logE("MatchData.event.toJson Exception: " + e.message)
				error.emit(R.string.fail_save_matches)
			}
			return ret
		}
	}

}

fun String?.isHome(): Boolean = this == HOME_ID
fun EventWhat.pretty(): String {
	return when(this) {
		EventWhat.START -> "Start"
		EventWhat.END -> "End"
		EventWhat.TIME_OFF -> "Time off"
		EventWhat.RESUME -> "Resume"
		EventWhat.TRY -> "Try"
		EventWhat.CONVERSION -> "Conversion"
		EventWhat.PENALTY_TRY -> "Penalty try"
		EventWhat.GOAL -> "Goal"
		EventWhat.DROP_GOAL -> "Drop goal"
		EventWhat.PENALTY_GOAL -> "Penalty goal"
		EventWhat.YELLOW_CARD -> "Yellow card"
		EventWhat.RED_CARD -> "Red card"
		EventWhat.PENALTY -> "Penalty"
		EventWhat.REPLACEMENT -> "Replacement"
	}
}
fun String.toEventWhat(): EventWhat {
	return when(this) {
		"Start" -> EventWhat.START
		"End" -> EventWhat.END
		"Time off" -> EventWhat.TIME_OFF
		"Resume" -> EventWhat.RESUME
		"Try" -> EventWhat.TRY
		"Conversion" -> EventWhat.CONVERSION
		"Penalty try" -> EventWhat.PENALTY_TRY
		"Goal" -> EventWhat.GOAL
		"Drop goal" -> EventWhat.DROP_GOAL
		"Penalty goal" -> EventWhat.PENALTY_GOAL
		"Yellow card" -> EventWhat.YELLOW_CARD
		"Red card" -> EventWhat.RED_CARD
		"Penalty" -> EventWhat.PENALTY
		"Replacement" -> EventWhat.REPLACEMENT
		else -> EventWhat.START
	}
}

class MatchType {
	var name by mutableStateOf("15s")
	var periodTime by mutableIntStateOf(40)
	var periodCount by mutableIntStateOf(2)
	var sinbin by mutableIntStateOf(10)
	var pointsTry by mutableIntStateOf(5)
	var pointsCon by mutableIntStateOf(2)
	var pointsGoal by mutableIntStateOf(3)
	var clockPK by mutableIntStateOf(60)
	var clockCon by mutableIntStateOf(60)
	var clockRestart by mutableIntStateOf(0)

	fun updateFields() {
		when(name) {
			"15s" -> {
				periodTime = 40
				periodCount = 2
				sinbin = 10
				pointsTry = 5
				pointsCon = 2
				pointsGoal = 3
				clockPK = 60
				clockCon = 60
				clockRestart = 0
			}
			"10s" -> {
				periodTime = 10
				periodCount = 2
				sinbin = 2
				pointsTry = 5
				pointsCon = 2
				pointsGoal = 3
				clockPK = 30
				clockCon = 30
				clockRestart = 0
			}
			"7s" -> {
				periodTime = 7
				periodCount = 2
				sinbin = 2
				pointsTry = 5
				pointsCon = 2
				pointsGoal = 3
				clockPK = 30
				clockCon = 30
				clockRestart = 30
			}
			"beach 7s" -> {
				periodTime = 7
				periodCount = 2
				sinbin = 2
				pointsTry = 1
				pointsCon = 0
				pointsGoal = 0
				clockPK = 15
				clockCon = 0
				clockRestart = 0
			}
			"beach 5s" -> {
				periodTime = 5
				periodCount = 2
				sinbin = 2
				pointsTry = 1
				pointsCon = 0
				pointsGoal = 0
				clockPK = 15
				clockCon = 0
				clockRestart = 0
			}
			else -> {
				MatchStore.customMatchTypes.firstOrNull { it.name == name }?.let {
					periodTime = it.periodTime
					periodCount = it.periodCount
					sinbin = it.sinbin
					pointsTry = it.pointsTry
					pointsCon = it.pointsCon
					pointsGoal = it.pointsGoal
					clockPK = it.clockPK
					clockCon = it.clockCon
					clockRestart = it.clockRestart
				}
			}
		}
	}
	constructor (name: String) {
		this.name = name
		updateFields()
	}
	constructor (original: MatchType) {
		name = original.name
		periodTime = original.periodTime
		periodCount = original.periodCount
		sinbin = original.sinbin
		pointsTry = original.pointsTry
		pointsCon = original.pointsCon
		pointsGoal = original.pointsGoal
		clockPK = original.clockPK
		clockCon = original.clockCon
		clockRestart = original.clockRestart
	}
	constructor (matchTypeJson: JSONObject) {
		name = matchTypeJson.optString("name", "15s")
		periodTime = matchTypeJson.optInt("period_time", 40)
		periodCount = matchTypeJson.optInt("period_count", 2)
		sinbin = matchTypeJson.optInt("sinbin", 10)
		pointsTry = matchTypeJson.optInt("points_try", 5)
		pointsCon = matchTypeJson.optInt("points_con", 2)
		pointsGoal = matchTypeJson.optInt("points_goal", 3)
		clockPK = matchTypeJson.optInt("clock_pk", 60)
		clockCon = matchTypeJson.optInt("clock_con", 60)
		clockRestart = matchTypeJson.optInt("clock_restart", 0)
	}
	constructor (sp: SharedPreferences) {
		name = sp.getString("name", null) ?: name
		periodTime = sp.getInt("periodTime", periodTime)
		periodCount = sp.getInt("periodCount", periodCount)
		sinbin = sp.getInt("sinbin", sinbin)
		pointsTry = sp.getInt("pointsTry", pointsTry)
		pointsCon = sp.getInt("pointsCon", pointsCon)
		pointsGoal = sp.getInt("pointsGoal", pointsGoal)
		clockPK = sp.getInt("clockPK", clockPK)
		clockCon = sp.getInt("clockCon", clockCon)
		clockRestart = sp.getInt("clockRestart", clockRestart)
	}
	fun store(spe: SharedPreferences.Editor) {
		spe.putString("name", name)
			.putInt("periodTime", periodTime)
			.putInt("periodCount", periodCount)
			.putInt("sinbin", sinbin)
			.putInt("pointsTry", pointsTry)
			.putInt("pointsCon", pointsCon)
			.putInt("pointsGoal", pointsGoal)
			.putInt("clockPK", clockPK)
			.putInt("clockCon", clockCon)
			.putInt("clockRestart", clockRestart)
			.apply()
	}
	fun toJson(): JSONObject {
		try {
			return JSONObject().put("name", name)
				.put("period_time", periodTime)
				.put("period_count", periodCount)
				.put("sinbin", sinbin)
				.put("points_try", pointsTry)
				.put("points_con", pointsCon)
				.put("points_goal", pointsGoal)
				.put("clock_pk", clockPK)
				.put("clock_con", clockCon)
				.put("clock_restart", clockRestart)
		} catch (e: Exception) { logE("MatchType.toJson Exception: " + e.message) }
		return JSONObject()
	}
	fun gotWatchSettings(settings: JSONObject) {
		name = settings.optString("match_type", name)
		periodTime = settings.optInt("period_time", periodTime)
		periodCount = settings.optInt("period_count", periodCount)
		sinbin = settings.optInt("sinbin", sinbin)
		pointsTry = settings.optInt("points_try", pointsTry)
		pointsCon = settings.optInt("points_con", pointsCon)
		pointsGoal = settings.optInt("points_goal", pointsGoal)
		clockPK = settings.optInt("clock_pk", clockPK)
		clockCon = settings.optInt("clock_con", clockCon)
		clockRestart = settings.optInt("clock_restart", clockRestart)
	}
}

class PrepData {
	var manualUpdate = false
	var homeName by mutableStateOf("")
	var homeColor by mutableStateOf("red")
	var awayName by mutableStateOf("")
	var awayColor by mutableStateOf("blue")

	var manualUpdateWatch = false
	var showWatchSettings by mutableStateOf(false)
	var keepScreenOn by mutableStateOf(true)
	fun toggleKeepScreenOn() {
		keepScreenOn = !keepScreenOn
		manualUpdateWatch = true
	}
	var timerType by mutableStateOf(false)
	fun toggleTimerType() {
		timerType = !timerType
		manualUpdateWatch = true
	}
	var recordPlayer by mutableStateOf(false)
	fun toggleRecordPlayer() {
		recordPlayer = !recordPlayer
		manualUpdateWatch = true
	}
	var recordPens by mutableStateOf(false)
	fun toggleRecordPens() {
		recordPens = !recordPens
		manualUpdateWatch = true
	}
	var delayEnd by mutableStateOf(true)
	fun toggleDelayEnd() {
		delayEnd = !delayEnd
		manualUpdateWatch = true
	}

	constructor()
	constructor (sp: SharedPreferences) {
		homeName = sp.getString("homeName", null) ?: homeName
		homeColor = sp.getString("homeColor", null) ?: homeColor
		awayName = sp.getString("awayName", null) ?: awayName
		awayColor = sp.getString("awayColor", null) ?: awayColor
		keepScreenOn = sp.getBoolean("keepScreenOn", keepScreenOn)
		timerType = sp.getBoolean("timerType", timerType)
		recordPlayer = sp.getBoolean("recordPlayer", recordPlayer)
		recordPens = sp.getBoolean("recordPens", recordPens)
		delayEnd = sp.getBoolean("delayEnd", delayEnd)
	}
	
	fun store(spe: SharedPreferences.Editor) {
		spe.putString("homeName", homeName)
			.putString("homeColor", homeColor)
			.putString("awayName", awayName)
			.putString("awayColor", awayColor)
			.putBoolean("keepScreenOn", keepScreenOn)
			.putBoolean("timerType", timerType)
			.putBoolean("recordPlayer", recordPlayer)
			.putBoolean("recordPens", recordPens)
			.putBoolean("delayEnd", delayEnd)
			.apply()
	}

	fun gotWatchSettings(settings: JSONObject) {
		if(!manualUpdate) {
			homeName = settings.optString("home_name", homeName)
			if(homeName == HOME_ID) homeName = ""
			homeColor = settings.optString("home_color", homeColor)
			awayName = settings.optString("away_name", awayName)
			if(awayName == AWAY_ID) awayName = ""
			awayColor = settings.optString("away_color", awayColor)
		}
		if(!manualUpdateWatch) {
			keepScreenOn = settings.optBoolean("screen_on", keepScreenOn)
			timerType = settings.optInt("timer_type", if(timerType) 1 else 0) == 1
			recordPlayer = settings.optBoolean("record_player", recordPlayer)
			recordPens = settings.optBoolean("record_pens", recordPens)
			delayEnd = settings.optBoolean("delay_end", delayEnd)
		}
	}
}
