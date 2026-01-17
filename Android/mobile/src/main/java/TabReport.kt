/*
 * Copyright 2020-2026 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.windkracht8.rugbyrefereewatch

import android.content.res.Configuration
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.windkracht8.rugbyrefereewatch.MatchData.EventWhat
import org.json.JSONObject

const val VIEW_TYPE_STANDARD = 0
const val VIEW_TYPE_FULL = 1
const val VIEW_TYPE_EDIT = 2

@Composable
fun TabReport(
	matchId: Long? = null,
	shareMatch: (Long, List<Boolean>) -> Unit,
	saveMatch: (MatchData) -> Unit
) {
	var viewType by remember { mutableIntStateOf(VIEW_TYPE_STANDARD) }
	val match = MatchStore.matches.lastOrNull { it.matchId == ( matchId ?: it.matchId) }

	if(match == null){
		Text(R.string.no_match)
		return
	}
	var matchEdit by remember(match) { mutableStateOf(MatchData(match)) }

	val showCons = match.home.cons > 0 || match.away.cons > 0
	val showPenTries = match.home.penTries > 0 || match.away.penTries > 0
	val showGoals = match.home.goals > 0 || match.away.goals > 0
	val showDropGoals = match.home.dropGoals > 0 || match.away.dropGoals > 0
	val showPenGoals = match.home.penGoals > 0 || match.away.penGoals > 0
	val showYellowCards = match.home.yellowCards > 0 || match.away.yellowCards > 0
	val showRedCards = match.home.redCards > 0 || match.away.redCards > 0
	val showPens = match.home.pens > 0 || match.away.pens > 0

	var shareDialog by remember { mutableStateOf(false) }

	Column(Modifier.fillMaxSize()) {
		Row(Modifier.fillMaxWidth()) {
			if(viewType == VIEW_TYPE_EDIT) {
				TextField(
					modifier = Modifier.weight(1f).padding(end = 2.dp),
					value = matchEdit.home.team,
					onValueChange = { matchEdit.home.team = it },
					textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
				)
				TextField(
					modifier = Modifier.weight(1f).padding(start = 2.dp),
					value = matchEdit.away.team,
					onValueChange = { matchEdit.away.team = it },
					textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
				)
			} else {
				Text(
					modifier = Modifier.weight(1f),
					text = match.home.team,
					fontWeight = FontWeight.Bold,
					textAlign = TextAlign.Center
				)
				Text(
					modifier = Modifier.weight(1f),
					text = match.away.team,
					fontWeight = FontWeight.Bold,
					textAlign = TextAlign.Center
				)
			}
		}
		if(viewType != VIEW_TYPE_EDIT) {
			Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
				Column(Modifier.weight(2f)) {
					Text(R.string.tries)
					if(showCons) Text(R.string.conversions)
					if(showPenTries) Text(R.string.pen_tries)
					if(showGoals) Text(R.string.goals)
					if(showDropGoals) Text(R.string.drop_goals)
					if(showPenGoals) Text(R.string.pen_goals)
					if(showYellowCards) Text(R.string.yellow_cards)
					if(showRedCards) Text(R.string.red_cards)
					if(showPens) Text(R.string.penalties)
				}
				Column(
					modifier = Modifier.weight(1f).padding(end = 10.dp),
					horizontalAlignment = Alignment.End
				) {
					Text(match.home.tries.toString())
					if(showCons) Text(match.home.cons.toString())
					if(showPenTries) Text(match.home.penTries.toString())
					if(showGoals) Text(match.home.goals.toString())
					if(showDropGoals) Text(match.home.dropGoals.toString())
					if(showPenGoals) Text(match.home.penGoals.toString())
					if(showYellowCards) Text(match.home.yellowCards.toString())
					if(showRedCards) Text(match.home.redCards.toString())
					if(showPens) Text(match.home.pens.toString())
					HorizontalDivider(thickness = 2.dp, color = colorScheme.onSurface)
					Text(
						text = match.home.tot.toString(),
						fontWeight = FontWeight.Bold
					)
				}
				Column(Modifier.weight(2f).padding(start = 10.dp)) {
					Text(R.string.tries)
					if(showCons) Text(R.string.conversions)
					if(showPenTries) Text(R.string.pen_tries)
					if(showGoals) Text(R.string.goals)
					if(showDropGoals) Text(R.string.drop_goals)
					if(showPenGoals) Text(R.string.pen_goals)
					if(showYellowCards) Text(R.string.yellow_cards)
					if(showRedCards) Text(R.string.red_cards)
					if(showPens) Text(R.string.penalties)
				}
				Column(
					modifier = Modifier.weight(1f),
					horizontalAlignment = Alignment.End
				) {
					Text(match.away.tries.toString())
					if(showCons) Text(match.away.cons.toString())
					if(showPenTries) Text(match.away.penTries.toString())
					if(showGoals) Text(match.away.goals.toString())
					if(showDropGoals) Text(match.away.dropGoals.toString())
					if(showPenGoals) Text(match.away.penGoals.toString())
					if(showYellowCards) Text(match.away.yellowCards.toString())
					if(showRedCards) Text(match.away.redCards.toString())
					if(showPens) Text(match.away.pens.toString())
					HorizontalDivider(thickness = 2.dp, color = colorScheme.onSurface)
					Text(
						text = match.away.tot.toString(),
						fontWeight = FontWeight.Bold
					)
				}
			}
		}
		when(viewType) {
			VIEW_TYPE_STANDARD -> {
				val scoreWidth = getMaxWidth(match.events) { it.score }
				val timerWidth = getMaxWidth(match.events) { it.prettyTimer() } + 14.dp
				LazyColumn (Modifier.fillMaxWidth().weight(1f)) {
					items(match.events.filter { it.what != EventWhat.REPLACEMENT} ) {
						 ReportEventStandard(it, scoreWidth, timerWidth)
					}
				}
			}
			VIEW_TYPE_FULL -> {
				val timeWidth = getMaxWidth(match.events) { it.time } + 5.dp
				val timerWidth = getMaxWidth(match.events) { it.prettyTimerFull() } + 5.dp
				LazyColumn (Modifier.fillMaxWidth().weight(1f)) {
					items(match.events) {
						ReportEventFull(it, timeWidth, timerWidth)
					}
				}
			}
			VIEW_TYPE_EDIT -> {
				val eventTypes = Array(EventWhat.entries.size-4) {
					EventWhat.entries[it+2].pretty()
				}
				val topPadding = TextFieldDefaults.contentPaddingWithoutLabel().calculateTopPadding()
				LazyColumn (Modifier.fillMaxWidth().weight(1f)) {
					items(matchEdit.events.filter {
						it.what !in setOf(EventWhat.TIME_OFF, EventWhat.RESUME, EventWhat.START,
							EventWhat.END, EventWhat.REPLACEMENT)
					}) { ReportEventEdit(it, eventTypes, topPadding) }
				}
			}
		}
		Row {
			if(viewType == VIEW_TYPE_EDIT) {
				TextButton(
					modifier = Modifier.weight(1f),
					onClick = {
						matchEdit = MatchData(match)
						viewType = VIEW_TYPE_STANDARD
					}
				) { Text(stringResource(R.string.cancel).lowercase()) }
				TextButton(
					modifier = Modifier.weight(1f),
					onClick = {
						saveMatch(matchEdit)
						viewType = VIEW_TYPE_STANDARD
					}
				) { Text(R.string.save) }
			} else {
				TextButton(
					modifier = Modifier.weight(1f),
					onClick = { viewType = VIEW_TYPE_EDIT }
				) { Text(R.string.edit) }
				TextButton(
					modifier = Modifier.weight(1f),
					onClick = { viewType = if (viewType == VIEW_TYPE_FULL) VIEW_TYPE_STANDARD else VIEW_TYPE_FULL }
				) { Text(R.string.view) }
				TextButton(
					modifier = Modifier.weight(1f),
					onClick = { shareDialog = true }
				) { Text(R.string.share) }
			}
		}
	}
	if (shareDialog) {
		var time by remember { mutableStateOf(false) }
		var pens by remember { mutableStateOf(false) }
		var clock by remember { mutableStateOf(false) }
		AlertDialog(
			title = { Text(R.string.share_dialog_title) },
			text = {
				Column {
					Row(verticalAlignment = Alignment.CenterVertically) {
						Text(
							modifier = Modifier.weight(1f).clickable(enabled = true, onClick = { time = !time }),
							text = stringResource(R.string.share_dialog_time)
						)
						Switch(checked = time, onCheckedChange = { time = it })
					}
					Row(verticalAlignment = Alignment.CenterVertically) {
						Text(
							modifier = Modifier.weight(1f).clickable(enabled = true, onClick = { pens = !pens }),
							text = stringResource(R.string.share_dialog_pens)
						)
						Switch(checked = pens, onCheckedChange = { pens = it })
					}
					Row(verticalAlignment = Alignment.CenterVertically) {
						Text(
							modifier = Modifier.weight(1f).clickable(enabled = true, onClick = { clock = !clock }),
							text = stringResource(R.string.share_dialog_clock)
						)
						Switch(checked = clock, onCheckedChange = { clock = it })
					}
				}
			},
			onDismissRequest = { shareDialog = false },
			confirmButton = {
				TextButton(
					onClick = {
						shareMatch(match.matchId, listOf(time, pens, clock))
						shareDialog = false
					}
				) { Text("Ok") }
			},
			dismissButton = {
				TextButton(onClick = { shareDialog = false }) { Text(R.string.cancel) }
			}
		)
	}
}

@Composable
fun ReportEventStandard(event: MatchData.Event, scoreWidth: Dp, timerWidth: Dp) {
	when(event.what) {
		EventWhat.TIME_OFF, EventWhat.RESUME -> {}
		EventWhat.START -> {
			Text(
				modifier = Modifier.fillMaxWidth(),
				text = event.prettyPeriod(),
				textAlign = TextAlign.Center
			)
		}
		EventWhat.END -> {
			Row(Modifier.fillMaxWidth()) {
				Text(
					modifier = Modifier.weight(1f),
					text = event.score.substringBefore(":"),
					textAlign = TextAlign.End
				)
				Text(
					modifier = Modifier.padding(horizontal = 5.dp),
					text = event.prettyPeriod(),
					textAlign = TextAlign.Center,
					maxLines = 1
				)
				Text(
					modifier = Modifier.weight(1f),
					text = event.score.substringAfter(":")
				)
			}
			Spacer(Modifier.height(12.dp))
		}
		else -> {
			val whatWithWho = if (event.who == null) event.what.pretty()
								else event.what.pretty() + " ${event.who}"
			Row(Modifier.fillMaxWidth()) {
				Text(
					modifier = Modifier.weight(1f),
					text = if (event.isHome == true) whatWithWho else "",
					textAlign = TextAlign.End
				)
				Text(
					modifier = Modifier.width(timerWidth).padding(end = 6.dp),
					text = if (event.isHome == true) event.prettyTimer() else "",
					textAlign = TextAlign.End
				)
				Text(
					modifier = Modifier.width(scoreWidth),
					text = event.score,
					textAlign =
						if(event.score.indexOf(':') == 2) TextAlign.Start
						else TextAlign.Center
				)
				Text(
					modifier = Modifier.width(timerWidth).padding(start = 6.dp),
					text = if (event.isHome == false) event.prettyTimer() else ""
				)
				Text(
					modifier = Modifier.weight(1f),
					text = if (event.isHome == false) whatWithWho else ""
				)
			}
			val reason = event.reason
			if (reason != null) {
				Text(
					modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp),
					text = reason,
					textAlign = TextAlign.Center
				)
			}
		}
	}
}

@Composable
fun ReportEventFull(event: MatchData.Event, timeWidth: Dp, timerWidth: Dp) {
	var what = when (event.what) {
		EventWhat.START -> event.prettyPeriod()
		EventWhat.END -> event.prettyPeriod() + " ${event.score}"
		else -> event.what.pretty()
	}
	event.teamName?.let { what += " $it" }
	event.who?.let { what += " $it" }
	what += LocalContext.current.replacementString(event)

	Row(Modifier.fillMaxWidth()){
		Text(
			modifier = Modifier.width(timeWidth),
			text = event.time
		)
		Text(
			modifier = Modifier.width(timerWidth).padding(end = 5.dp),
			text = event.prettyTimerFull(),
			textAlign = TextAlign.End
		)
		Text(
			modifier = Modifier.weight(1f),
			text = what
		)
	}
	val reason = event.reason
	if(reason != null) {
		Text(
			modifier = Modifier.fillMaxWidth(),
			text = reason
		)
	}
}

@Composable
fun ReportEventEdit(event: MatchData.Event, eventTypes: Array<String>, topPadding: Dp) {
	Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
		IconButton(onClick = event::delete) {
			Icon(
				imageVector = Icons.Default.Delete,
				contentDescription = "delete event"
			)
		}
		Spinner(
			options = eventTypes,
			value = event.what.pretty(),
			onSelected = { event.what = it.toEventWhat() },
			modifier = Modifier.weight(1f)
		)
		Text(
			modifier = Modifier
				.width(64.dp)
				.padding(horizontal = 4.dp)
				.border(1.dp, colorScheme.outline, MaterialTheme.shapes.small)
				.padding(vertical = topPadding)
				.clickable { event.isHome = !(event.isHome ?: true) },
			text = if (event.isHome ?: true) MatchData.HOME_ID else MatchData.AWAY_ID,
			textAlign = TextAlign.Center
		)
		IntField(
			modifier = Modifier.width(65.dp),
			value = event.who ?: 0,
			onValueChange = { event.who = if(it == 0) null else it },
			canBe0 = true
		)
	}
	if(event.what in setOf(EventWhat.YELLOW_CARD, EventWhat.RED_CARD)) {
		TextField(
			modifier = Modifier.fillMaxWidth(),
			value = event.reason ?: "",
			onValueChange = { event.reason = it },
			placeholder = { Text(R.string.reason_hint) }
		)
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
//Also used in TabPrepare
fun Spinner(
	modifier: Modifier = Modifier,
	options: Array<String>,
	value: String, onSelected: (String) -> Unit
) {
	var expanded by remember { mutableStateOf(false) }
	ExposedDropdownMenuBox(
		modifier = modifier,
		expanded = expanded,
		onExpandedChange = { expanded = !expanded }
	) {
		OutlinedTextField(
			modifier = modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
			value = value,
			onValueChange = {},
			readOnly = true,
			maxLines = 1
		)
		ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
			options.forEach { selected ->
				DropdownMenuItem(
					text = { Text(selected) },
					onClick = { onSelected(selected); expanded = false }
				)
			}
		}
	}
}

@Composable
//Also used in TabPrepare
fun IntField(modifier: Modifier = Modifier, value: Int, onValueChange: (Int) -> Unit, canBe0: Boolean){
	var isError by remember { mutableStateOf(false) }
	TextField(
		modifier = modifier,
		value = if(value == 0) "" else value.toString(),
		onValueChange = {
			try {
				onValueChange(it.toInt())
				isError = false
			}
			catch(_: Exception) {
				onValueChange(0)
				if(!canBe0) isError = true
			}
		},
		maxLines = 1,
		isError = isError,
		keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
	)
}

@Composable
fun getMaxWidth(events: SnapshotStateList<MatchData.Event>, text: (MatchData.Event) -> String): Dp{
	val textMeasurer = rememberTextMeasurer()
	var maxWidth = 0
	events.forEach {
		val textLayoutResult = textMeasurer.measure(text = text(it), style = LocalTextStyle.current)
		if (textLayoutResult.size.width > maxWidth) maxWidth = textLayoutResult.size.width
	}
	return with(LocalDensity.current) { maxWidth.toDp() }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, apiLevel = 35)
@Composable
fun PreviewTabReport() {
	MatchStore.matches.add(MatchData(JSONObject("{\"matchid\":1741956291022,\"format\":3,\"settings\":{\"match_type\":\"7s\",\"period_time\":7,\"period_count\":2,\"sinbin\":2,\"points_try\":5,\"points_con\":2,\"points_goal\":3,\"pk_clock\":30,\"conv_clock\":30,\"restart_clock\":30},\"home\":{\"id\":\"home\",\"team\":\"RSA\",\"color\":\"green\",\"tot\":26,\"tries\":4,\"cons\":3,\"pen_tries\":0,\"goals\":0,\"yellow_cards\":1,\"red_cards\":0,\"pens\":0,\"kickoff\":true},\"away\":{\"id\":\"away\",\"team\":\"FRA\",\"color\":\"blue\",\"tot\":14,\"tries\":2,\"cons\":2,\"pen_tries\":0,\"goals\":0,\"yellow_cards\":0,\"red_cards\":0,\"pens\":0,\"kickoff\":false},\"events\":[{\"id\":1741956291026,\"time\":\"13:44:51\",\"timer\":0,\"period\":1,\"what\":\"START\",\"team\":\"home\"},{\"id\":1741956391525,\"time\":\"13:46:31\",\"timer\":99,\"period\":1,\"what\":\"TRY\",\"team\":\"away\",\"who\":123},{\"id\":1741956418149,\"time\":\"13:46:58\",\"timer\":126,\"period\":1,\"what\":\"CONVERSION\",\"team\":\"away\",\"who\":6},{\"id\":1741956520348,\"time\":\"13:48:40\",\"timer\":228,\"period\":1,\"what\":\"TRY\",\"team\":\"home\",\"who\":2},{\"id\":1741956537051,\"time\":\"13:48:57\",\"timer\":245,\"period\":1,\"what\":\"CONVERSION\",\"team\":\"home\",\"who\":6},{\"id\":1741956598237,\"time\":\"13:49:58\",\"timer\":306,\"period\":1,\"what\":\"TRY\",\"team\":\"home\",\"who\":5},{\"id\":1741956609468,\"time\":\"13:50:09\",\"timer\":317,\"period\":1,\"what\":\"CONVERSION\",\"team\":\"home\",\"who\":6},{\"id\":1741956736219,\"time\":\"13:52:16\",\"timer\":444,\"period\":1,\"what\":\"END\",\"score\":\"14:7\"},{\"id\":1741956813839,\"time\":\"13:53:33\",\"timer\":420,\"period\":2,\"what\":\"START\",\"team\":\"away\"},{\"id\":1741956878692,\"time\":\"13:54:38\",\"timer\":484,\"period\":2,\"what\":\"TRY\",\"team\":\"away\",\"who\":3},{\"id\":1741956888114,\"time\":\"13:54:48\",\"timer\":494,\"period\":2,\"what\":\"CONVERSION\",\"team\":\"away\",\"who\":5},{\"id\":1741956939965,\"time\":\"13:55:39\",\"timer\":545,\"period\":2,\"what\":\"TRY\",\"team\":\"home\",\"who\":11},{\"id\":1741956946243,\"time\":\"13:55:46\",\"timer\":552,\"period\":2,\"what\":\"CONVERSION\",\"team\":\"home\",\"who\":6},{\"id\":1741957001427,\"time\":\"13:56:41\",\"timer\":607,\"period\":2,\"what\":\"TRY\",\"team\":\"home\",\"who\":4},{\"id\":1741957002427,\"time\":\"13:56:42\",\"timer\":608,\"period\":2,\"what\":\"REPLACEMENT\",\"team\":\"home\",\"who_leave\":4,\"who_enter\":22},{\"id\":1741957123119,\"time\":\"13:58:43\",\"timer\":729,\"period\":2,\"what\":\"YELLOW CARD\",\"team\":\"home\",\"who\":10},{\"id\":1741957256934,\"time\":\"14:00:56\",\"timer\":861,\"period\":2,\"what\":\"END\",\"score\":\"26:14\"}]}")))
	W8Theme (null, null) { Surface { TabReport(null, {_: Long, _: List<Boolean> ->}) {} } }
}
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, apiLevel = 35)
@Composable
fun PreviewTabReportDay() {
	W8Theme (null, null) { Surface { TabReport(null, {_: Long, _: List<Boolean> ->}) {} } }
}
