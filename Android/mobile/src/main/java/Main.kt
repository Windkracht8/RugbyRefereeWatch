/*
 * Copyright 2020-2025 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.windkracht8.rugbyrefereewatch

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.windkracht8.rugbyrefereewatch.MatchData.Companion.AWAY_ID
import com.windkracht8.rugbyrefereewatch.MatchData.Companion.HOME_ID
import com.windkracht8.rugbyrefereewatch.MatchData.EventWhat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val error = MutableSharedFlow<Int>()
class Main : ComponentActivity() {
	var commsBTStatus by mutableStateOf(Comms.status.value)
	lateinit var matchType: MatchType
	lateinit var prepData: PrepData

	override fun onCreate(savedInstanceState: Bundle?) {
		installSplashScreen()
		super.onCreate(savedInstanceState)
		val sharedPreferences = getPreferences(MODE_PRIVATE)
		matchType = MatchType(sharedPreferences)
		prepData = PrepData(sharedPreferences)

		enableEdgeToEdge()
		setContent {
			W8Theme { Surface { Home(
				commsBTStatus,
				::onIconClick,
				::onImportClick,
				::deleteMatches,
				::exportMatches,
				::shareMatch,
				::saveMatch,
				matchType,
				prepData,
				::onPrepareClicked,
				::onSaveMatchType,
				::onDeleteMatchType
			) } }
		}

		lifecycleScope.launch {
			Comms.status.collect { status ->
				logD("Main: CommsBT status change: $status")
				commsBTStatus = Comms.status.value
				if (commsBTStatus == Comms.Status.CONNECTING) {
					startActivity(Intent(this@Main, DeviceConnect::class.java))
				}
			}
		}
		lifecycleScope.launch { Comms.watchMatch.collect {
			if(MatchStore.matches.any { it2 -> it2.matchId == it.matchId }) return@collect
			MatchStore.matches.add(it)
			runInBackground { MatchStore.storeMatches(this@Main) }
		} }
		lifecycleScope.launch { Comms.watchSettings.collect {
			prepData.gotWatchSettings(it)
			if(!prepData.manualUpdate) matchType.gotWatchSettings(it)
		} }
		lifecycleScope.launch { error.collect { toast(it) } }
		runInBackground { MatchStore.read(this@Main) }

		Permissions.checkPermissions(this)
		if (!Permissions.hasBT) startActivity(
			Intent(this, Permissions::class.java)
		)
	}
	override fun onResume() {
		super.onResume()
		if (Permissions.hasBT) {
			registerReceiver(
				btBroadcastReceiver,
				IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
			)
			if (Comms.status.value == null) runInBackground { Comms.start(this@Main) }
		}
	}
	override fun onPause() {
		super.onPause()
		tryIgnore { unregisterReceiver(btBroadcastReceiver) }
	}
	override fun onDestroy() {
		super.onDestroy()
		runInBackground {
			val spe = getPreferences(MODE_PRIVATE).edit()
			matchType.store(spe)
			prepData.store(spe)
			Comms.stop()
			Comms.onDestroy(this)
		}
	}
	val btBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context?, intent: Intent) {
			if (BluetoothAdapter.ACTION_STATE_CHANGED == intent.action) {
				val btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
				if (btState == BluetoothAdapter.STATE_TURNING_OFF) {
					Comms.onError(R.string.fail_BT_off)
					Comms.stop()
				} else if (btState == BluetoothAdapter.STATE_ON) {
					runInBackground { Comms.start(this@Main) }
				}
			}
		}
	}

	fun onIconClick() {
		logD("onIconClick: " + Comms.status.value)
		if (!Permissions.hasBT) {
			startActivity(Intent(this, Permissions::class.java))
		} else if (Comms.status.value == Comms.Status.DISCONNECTED) {
			startActivity(Intent(this, DeviceSelect::class.java))
		} else {
			Comms.stop()
		}
	}
	fun onImportClick() { importMatchesResult.launch(arrayOf("application/json")) }
	val importMatchesResult = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
		logD("Main.importMatchesResult: $uri")
		if (uri == null) {
			logE("Main.importMatchesResult empty uri")
			toast(R.string.fail_import)
			return@registerForActivityResult
		}
		runInBackground { MatchStore.importMatches(this@Main, uri) }
	}
	fun deleteMatches(matchIds: Set<Long>) {
		logD("Main.deleteMatches: $matchIds")
		//run on UI, because it will update matches, and this need to happen on the UI thread
		MatchStore.deleteMatches(this, matchIds)
		Comms.syncIfConnected()
	}
	fun exportMatches(matchIds: Set<Long>) {
		logD("Main.exportMatches: $matchIds")
		exportMatchIds = matchIds
		exportMatchesResult.launch("matches.json")
	}
	var exportMatchIds: Set<Long>? = null
	val exportMatchesResult = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
		logD("Main.exportMatchesResult: $uri")
		val exportMatchIds2 = exportMatchIds
		if (uri == null || exportMatchIds2 == null) {
			logD("Main.exportMatchesResult empty uri")
			toast(R.string.fail_export)
			return@registerForActivityResult
		}
		runInBackground {
			MatchStore.exportMatches(this@Main, uri, exportMatchIds2)
			exportMatchIds = null
		}
	}
	fun shareMatch(matchId: Long, eventTypes: List<Boolean>) {
		logD("Main.shareMatch: $matchId")
		val match = MatchStore.matches.firstOrNull{ it.matchId == matchId }
		if (match == null) {
			logE("Main.shareMatch match not found: $matchId")
			toast(R.string.fail_share)
			return
		}
		val intent = Intent()
		intent.setAction(Intent.ACTION_SEND)
		intent.setType("text/plain")
		intent.putExtra(Intent.EXTRA_SUBJECT, getShareSubject(match))
		intent.putExtra(Intent.EXTRA_TEXT, getShareBody(match, eventTypes))
		try {
			startActivity(Intent.createChooser(intent, getString(R.string.share_report)))
		} catch (e: java.lang.Exception) {
			logE("Main.shareMatch Exception: " + e.message)
			toast(R.string.fail_share)
		}
	}
	fun getShareSubject(match: MatchData): String {
		val date = SimpleDateFormat("E dd-MM-yyyy HH:mm", Locale.getDefault())
			.format(Date(match.matchId))
		return getString(R.string.match_report) + " $date ${match.home.team} ${match.home.tot} v ${match.away.team} ${match.away.tot}"
	}
	fun getShareBody(match: MatchData, eventTypes: List<Boolean>): String {
		//eventTypes[0]: show time on/off
		//eventTypes[1]: show penalties
		//eventTypes[2]: show clock time
		val shareBody = StringBuilder()
		shareBody.append(getShareSubject(match)).append("\n\n")

		val scoreHome = StringBuilder()
		scoreHome.append("${match.home.team}\n  " + getString(R.string.tries) + ": ${match.home.tries}\n")

		val scoreAway = StringBuilder()
		scoreAway.append("${match.away.team}\n  " + getString(R.string.tries) + ": ${match.away.tries}\n")

		fun scoreLine(home: Int, away: Int, label: Int) {
			if (home > 0 || away > 0) {
				scoreHome.append("  " + getString(label) + ": $home\n")
				scoreAway.append("  " + getString(label) + ": $away\n")
			}
		}
		scoreLine(match.home.cons, match.away.cons, R.string.conversions)
		scoreLine(match.home.penTries, match.away.penTries, R.string.pen_tries)
		scoreLine(match.home.goals, match.away.goals, R.string.goals)
		scoreLine(match.home.penGoals, match.away.penGoals, R.string.pen_goals)
		scoreLine(match.home.dropGoals, match.away.dropGoals, R.string.drop_goals)
		scoreLine(match.home.yellowCards, match.away.yellowCards, R.string.yellow_cards)
		scoreLine(match.home.redCards, match.away.redCards, R.string.red_cards)
		if (eventTypes[1]) scoreLine(match.home.pens, match.away.pens, R.string.penalties)

		scoreHome.append("  " + getString(R.string.total) + ": ${match.home.tot}\n")
		scoreAway.append("  " + getString(R.string.total) + ": ${match.away.tot}\n")

		shareBody.append(scoreHome).append("\n").append(scoreAway).append("\n")

		val doubleDigitTime = match.periodTime >= 10
		match.events.forEach { event ->
			if ((!eventTypes[0] && event.what in setOf(EventWhat.TIME_OFF, EventWhat.RESUME)) ||
				(!eventTypes[2] && event.what == EventWhat.PENALTY)
			) return@forEach

			if (eventTypes[1]) shareBody.append("${event.time}  ")

			val timer = event.prettyTimerFull()
			if (doubleDigitTime && timer.length == 4) shareBody.append("0")
			shareBody.append("$timer  ")

			if (event.what in setOf(EventWhat.START, EventWhat.END)) {
				shareBody.append(event.prettyPeriod())
			} else {
				shareBody.append(event.what.pretty())
			}

			event.teamName?.let { shareBody.append(" $it") }
			event.who?.let { shareBody.append(" $it") }
			event.reason?.let { shareBody.append("\n      $it") }
			if(event.what == EventWhat.END) shareBody.append(" ${event.score}\n")
			shareBody.append("\n")
		}
		//logD(shareBody.toString())
		return shareBody.toString()
	}
	fun saveMatch(matchData: MatchData) {
		matchData.recalc()
		MatchStore.saveMatch(this, matchData)
	}

	fun onPrepareClicked() {
		logD("onPrepareClicked")
		val settings = JSONObject()
		settings.put("home_name", if(prepData.homeName == "") HOME_ID else prepData.homeName)
		settings.put("home_color", prepData.homeColor)
		settings.put("away_name", if(prepData.awayName == "") AWAY_ID else prepData.awayName)
		settings.put("away_color", prepData.awayColor)
		settings.put("match_type", matchType.name)
		settings.put("period_time", matchType.periodTime)
		settings.put("period_count", matchType.periodCount)
		settings.put("sinbin", matchType.sinbin)
		settings.put("points_try", matchType.pointsTry)
		settings.put("points_con", matchType.pointsCon)
		settings.put("points_goal", matchType.pointsGoal)
		settings.put("clock_pk", matchType.clockPK)
		settings.put("clock_con", matchType.clockCon)
		settings.put("clock_restart", matchType.clockRestart)
		val sendWatchSettings = prepData.manualUpdateWatch || prepData.showWatchSettings
		if(sendWatchSettings) settings.put("screen_on", prepData.keepScreenOn)
		if(sendWatchSettings) settings.put("timer_type", if(prepData.timerType) 1 else 0)
		if(sendWatchSettings) settings.put("record_player", prepData.recordPlayer)
		if(sendWatchSettings) settings.put("record_pens", prepData.recordPens)
		if(sendWatchSettings) settings.put("delay_end", prepData.delayEnd)

		Comms.sendRequestPrep(settings)
	}
	fun onSaveMatchType(name: String) {
		logD("onSaveMatchType($name) " + matchType.toJson())
		if(!MatchStore.customMatchTypeNames.contains(name)) {
			MatchStore.customMatchTypeNames.add(name)
			MatchStore.customMatchTypeNames.sort()
		}
		MatchStore.customMatchTypes.removeIf { it.name == name }
		matchType.name = name
		MatchStore.customMatchTypes.add(MatchType(matchType))
		runInBackground {
			MatchStore.storeCustomMatchTypes(this@Main)
			Comms.syncIfConnected()
		}
	}
	fun onDeleteMatchType() {
		logD("onDeleteMatchType")
		MatchStore.customMatchTypeNames.remove(matchType.name)
		MatchStore.customMatchTypes.removeIf { it.name == matchType.name }
		matchType.name = "15s"
		matchType.updateFields()
		runInBackground {
			MatchStore.storeCustomMatchTypes(this@Main)
			Comms.syncIfConnected()
		}
	}
}
