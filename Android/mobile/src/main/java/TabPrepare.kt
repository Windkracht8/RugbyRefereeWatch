/*
 * Copyright 2020-2026 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.windkracht8.rugbyrefereewatch

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun TabPrepare(
	commsBTStatus: Comms.Status?,
	matchType: MatchType,
	prepData: PrepData,
	onPrepareClicked: () -> Unit,
	onSaveMatchType: (String) -> Unit,
	onDeleteMatchType: () -> Unit
) {
	val context: Context = LocalContext.current
	val saveMatchType = remember { mutableStateOf(false) }
	var showMatchTypeDetails by remember { mutableStateOf(false) }
	var changeTeamIsHome by remember { mutableStateOf(true) }
	val changeTeamName = remember { mutableStateOf(false) }
	var changeTeamColor by remember { mutableStateOf(false) }
	var changeMatchType by remember { mutableStateOf(false) }
	val changeValue = remember { mutableStateOf(false) }
	var changeValueType by remember { mutableStateOf<ValueTypes?>(null) }
	var changeValueValue by remember { mutableIntStateOf(0) }
	var changeValueCanBe0 by remember { mutableStateOf(true) }

	val teamColors = stringArrayResource(R.array.team_colors)
	val standardMatchTypes = stringArrayResource(R.array.match_types)
	val matchTypeNames = standardMatchTypes + MatchStore.customMatchTypeNames

	LazyColumn(Modifier.fillMaxSize().padding(horizontal = 5.dp)) {
		item { OutlinedButton(
			modifier = Modifier.fillMaxWidth(),
			onClick = onPrepareClicked
		) {
			Text(
				text = stringResource(R.string.send_to_watch),
				color =
					if (commsBTStatus in setOf(Comms.Status.CONNECTED_BT, Comms.Status.CONNECTED_IQ)) colorScheme.primary
					else colorScheme.primary.copy(alpha = 0.5f)
			)
		} }
		item {
			Setting(R.string.match_type, matchType.name, 1) {
				changeMatchType = true
			}
		}
		item {
			Row (modifier = Modifier.fillMaxWidth()) {
				Column (modifier = Modifier.weight(1f)){
					Setting(R.string.home_name, prepData.homeName) {
						changeTeamIsHome = true
						changeTeamName.value = true
					}
					Setting(R.string.home_color, prepData.homeColor) {
						changeTeamIsHome = true
						changeTeamColor = true
					}
				}
				Column (modifier = Modifier.weight(1f)){
					Setting(R.string.away_name, prepData.awayName, 2) {
						changeTeamIsHome = false
						changeTeamName.value = true
					}
					Setting(R.string.away_color, prepData.awayColor, 2) {
						changeTeamIsHome = false
						changeTeamColor = true
					}
				}
			}
		}
		if (!standardMatchTypes.contains(matchType.name)) {
			item { OutlinedButton(
				modifier = Modifier.fillMaxWidth(),
				onClick = onDeleteMatchType
			) { Text(stringResource(R.string.del_match_type), color = colorScheme.error) } }
		}
		item { TextButton(
			modifier = Modifier.fillMaxWidth(),
			onClick = { showMatchTypeDetails = !showMatchTypeDetails }
		) {
			Text(if (showMatchTypeDetails) R.string.match_type_hide
			else R.string.match_type_show
			)
		} }
		if(showMatchTypeDetails) {
			item {
				OutlinedButton(
					modifier = Modifier.fillMaxWidth(),
					onClick = { saveMatchType.value = true }
				) { Text(R.string.save_match_type) }
			}
			item {
				Setting(
					title = R.string.period_time,
					subTitle = matchType.periodTime.toString(),
					onClick = {
						changeValueType = ValueTypes.PeriodTime
						changeValueValue = matchType.periodTime
						changeValueCanBe0 = false
						changeValue.value = true
					}
				)
			}
			item {
				Setting(
					title = R.string.period_count,
					subTitle = matchType.periodCount.toString(),
					onClick = {
						changeValueType = ValueTypes.PeriodCount
						changeValueValue = matchType.periodCount
						changeValueCanBe0 = false
						changeValue.value = true
					}
				)
			}
			item {
				Setting(
					title = R.string.sinbin,
					subTitle = matchType.sinbin.toString(),
					onClick = {
						changeValueType = ValueTypes.Sinbin
						changeValueValue = matchType.sinbin
						changeValueCanBe0 = true
						changeValue.value = true
					}
				)
			}
			item {
				Setting(
					title = R.string.points_try,
					subTitle = matchType.pointsTry.toString(),
					onClick = {
						changeValueType = ValueTypes.PointsTry
						changeValueValue = matchType.pointsTry
						changeValueCanBe0 = false
						changeValue.value = true
					}
				)
			}
			item {
				Setting(
					title = R.string.points_con,
					subTitle = matchType.pointsCon.toString(),
					onClick = {
						changeValueType = ValueTypes.PointsCon
						changeValueValue = matchType.pointsCon
						changeValueCanBe0 = true
						changeValue.value = true
					}
				)
			}
			item {
				Setting(
					title = R.string.points_goal,
					subTitle = matchType.pointsGoal.toString(),
					onClick = {
						changeValueType = ValueTypes.PointsGoal
						changeValueValue = matchType.pointsGoal
						changeValueCanBe0 = true
						changeValue.value = true
					}
				)
			}
			item {
				Setting(
					title = R.string.clock_pk,
					subTitle = matchType.clockPK.toString(),
					onClick = {
						changeValueType = ValueTypes.ClockPK
						changeValueValue = matchType.clockPK
						changeValueCanBe0 = true
						changeValue.value = true
					}
				)
			}
			item {
				Setting(
					title = R.string.clock_con,
					subTitle = matchType.clockCon.toString(),
					onClick = {
						changeValueType = ValueTypes.ClockCon
						changeValueValue = matchType.clockCon
						changeValueCanBe0 = true
						changeValue.value = true
					}
				)
			}
			item {
				Setting(
					title = R.string.clock_restart,
					subTitle = matchType.clockRestart.toString(),
					onClick = {
						changeValueType = ValueTypes.ClockRestart
						changeValueValue = matchType.clockRestart
						changeValueCanBe0 = true
						changeValue.value = true
					}
				)
			}
		}

		item { TextButton(
			modifier = Modifier.fillMaxWidth(),
			onClick = { prepData.showWatchSettings = !prepData.showWatchSettings }
		) {
			Text(
				if (prepData.showWatchSettings) R.string.watch_settings_hide
				else R.string.watch_settings_show
			)
		} }

		if(prepData.showWatchSettings) {
			item {
				Setting(
					title = R.string.screen,
					subTitle =
						if (prepData.keepScreenOn) stringResource(R.string.keep_on)
						else stringResource(R.string.auto_off),
					onClick = prepData::toggleKeepScreenOn
				)
			}
			item {
				Setting(
					title = R.string.timer_type,
					subTitle =//TIMER_TYPE_UP = 0
						if (prepData.timerType) stringResource(R.string.timer_type_down)
						else stringResource(R.string.timer_type_up),
					onClick = prepData::toggleTimerType
				)
			}
			item {
				SettingSwitch(
					title = R.string.record_player,
					checked = prepData.recordPlayer,
					onClick = prepData::toggleRecordPlayer
				)
			}
			item {
				SettingSwitch(
					title = R.string.record_pens,
					checked = prepData.recordPens,
					onClick = prepData::toggleRecordPens
				)
			}
			item {
				SettingSwitch(
					title = R.string.delay_end,
					checked = prepData.delayEnd,
					onClick = prepData::toggleDelayEnd
				)
			}
		}
	}
	if (changeTeamName.value) {
		StringInput(
			show = changeTeamName,
			title = if(changeTeamIsHome) R.string.home_name else R.string.away_name,
			value = if(changeTeamIsHome) prepData.homeName else prepData.awayName,
			onSave = { name ->
				if(changeTeamIsHome) prepData.homeName = name
				else prepData.awayName = name
				prepData.manualUpdate = true
			}
		)
	}
	if (changeTeamColor) {
		Dialog(onDismissRequest = { changeTeamColor = false }){
			LazyColumn (modifier = Modifier.background(color = colorScheme.background)){
				items(teamColors) { color ->
					TextButton(
						modifier = Modifier.requiredHeight(48.dp),
						onClick = {
							if(changeTeamIsHome) prepData.homeColor = color
							else prepData.awayColor = color
							prepData.manualUpdate = true
							changeTeamColor = false
						}
					){ Text(color) }
				}
			}
		}
	}
	if (changeMatchType) {
		Dialog(onDismissRequest = { changeMatchType = false }){
			LazyColumn (modifier = Modifier.background(color = colorScheme.background)){
				items(matchTypeNames) { matchTypeName ->
					TextButton(
						modifier = Modifier.requiredHeight(48.dp),
						onClick = {
							matchType.name = matchTypeName
							matchType.updateFields()
							prepData.manualUpdate = true
							changeMatchType = false
						}
					){ Text(matchTypeName) }
				}
			}
		}
	}
	if (saveMatchType.value) {
		StringInput(
			show = saveMatchType,
			title = R.string.save_match_type,
			value = if (standardMatchTypes.contains(matchType.name)) "" else matchType.name,
			onSave = { name ->
				if (name.isEmpty()) {
					context.toast(R.string.fail_empty_name)
				} else if (standardMatchTypes.contains(name)) {
					context.toast(R.string.fail_standard_match_type)
				} else {
					onSaveMatchType(name)
				}
			}
		)
	}
	if (changeValue.value) {
		IntInput(
			show = changeValue,
			title =
				when(changeValueType) {
					ValueTypes.PeriodTime -> R.string.period_time
					ValueTypes.PeriodCount -> R.string.period_count
					ValueTypes.Sinbin -> R.string.sinbin
					ValueTypes.PointsTry -> R.string.points_try
					ValueTypes.PointsCon -> R.string.points_con
					ValueTypes.PointsGoal -> R.string.points_goal
					ValueTypes.ClockPK -> R.string.clock_pk
					ValueTypes.ClockCon -> R.string.clock_con
					ValueTypes.ClockRestart -> R.string.clock_restart
					else -> R.string.period_time
				},
			value = changeValueValue,
			onSave = { newValue ->
				if(changeValueCanBe0 || newValue > 0) {
					when (changeValueType) {
						ValueTypes.PeriodTime -> matchType.periodTime = newValue
						ValueTypes.PeriodCount -> matchType.periodCount = newValue
						ValueTypes.Sinbin -> matchType.sinbin = newValue
						ValueTypes.PointsTry -> matchType.pointsTry = newValue
						ValueTypes.PointsCon -> matchType.pointsCon = newValue
						ValueTypes.PointsGoal -> matchType.pointsGoal = newValue
						ValueTypes.ClockPK -> matchType.clockPK = newValue
						ValueTypes.ClockCon -> matchType.clockCon = newValue
						ValueTypes.ClockRestart -> matchType.clockRestart = newValue
						else -> {}
					}
					prepData.manualUpdate = true
				} else {
					when (changeValueType) {
						ValueTypes.PeriodTime -> context.toast(R.string.time_period_empty)
						ValueTypes.PeriodCount -> context.toast(R.string.period_count_empty)
						ValueTypes.PointsTry -> context.toast(R.string.points_try_empty)
						else -> {}
					}
				}
			}
		)
	}
}

enum class ValueTypes{
	PeriodTime, PeriodCount, Sinbin, PointsTry, PointsCon, PointsGoal,
	ClockPK, ClockCon, ClockRestart
}

@Composable
fun Setting(
	title: Int,
	subTitle: String,
	align: Int = 0,
	onClick: () -> Unit
){
	Column(
		modifier = Modifier.fillMaxWidth().requiredHeight(48.dp).clickable(onClick = onClick),
		horizontalAlignment = when(align) {
			1 -> Alignment.CenterHorizontally
			2 -> Alignment.End
			else -> Alignment.Start
		}
	) {
		Text(
			text = stringResource(title),
			style = MaterialTheme.typography.titleMedium
		)
		if(subTitle.isNotEmpty()) {
			Text(
				text = if(subTitle == "0") "-" else subTitle,
				style = MaterialTheme.typography.bodyMedium,
				color = colorScheme.primary
			)
		}
	}
}

@Composable
fun SettingSwitch(
	title: Int,
	checked: Boolean,
	onClick: () -> Unit
){
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.requiredHeight(48.dp)
			.clickable(onClick = onClick),
		verticalAlignment = Alignment.CenterVertically
	){
		Text(
			modifier = Modifier.weight(1f),
			text = stringResource(title),
			style = MaterialTheme.typography.titleMedium
		)
		Switch(
			checked = checked,
			onCheckedChange = { onClick() }
		)
	}
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewTabPrepare() {
	W8Theme (null, null) { Surface { TabPrepare(
		commsBTStatus = Comms.Status.CONNECTED_BT,
		MatchType("15s"), PrepData(),
		{}, {}, {}
	) } }
}
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun PreviewTabPrepareDay() { PreviewTabPrepare() }
