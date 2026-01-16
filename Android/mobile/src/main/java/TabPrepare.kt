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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
	var saveMatchType by remember { mutableStateOf(false) }
	var showMatchTypeDetails by remember { mutableStateOf(false) }
	var changeTeam by remember { mutableStateOf(true) }
	var changeTeamName by remember { mutableStateOf(false) }
	var changeTeamColor by remember { mutableStateOf(false) }
	var changeMatchType by remember { mutableStateOf(false) }
	var changeValue by remember { mutableStateOf(false) }
	var changeValueType by remember { mutableStateOf<ValueTypes?>(null) }
	var changeValueValue by remember { mutableIntStateOf(0) }
	var changeValueCanBe0 by remember { mutableStateOf(true) }

	val teamColors = stringArrayResource(R.array.team_colors)
	val standardMatchTypes = stringArrayResource(R.array.match_types)
	val matchTypeNames = standardMatchTypes + MatchStore.customMatchTypeNames

	LazyColumn(Modifier.fillMaxSize().padding(horizontal = 5.dp)) {
		item { OutlinedButton(
			modifier = Modifier.fillMaxWidth(),
			onClick = {
				if(matchType.periodTime == 0) context.toast(R.string.time_period_empty)
				else if(matchType.periodCount == 0) context.toast(R.string.period_count_empty)
				else if(matchType.pointsTry == 0) context.toast(R.string.points_try_empty)
				else onPrepareClicked()
			}
		) {
			Text(
				text = stringResource(R.string.send_to_watch),
				color =
					if (commsBTStatus in setOf(Comms.Status.CONNECTED_BT, Comms.Status.CONNECTED_IQ)) colorScheme.primary
				else colorScheme.primary.copy(alpha = 0.5f)
			)
		} }
		item {
			Row (modifier = Modifier.fillMaxWidth()) {
				Column (modifier = Modifier.weight(1f)){
					Setting(R.string.home_name, prepData.homeName) {
						changeTeam = true
						changeTeamName = true
					}
					Setting(R.string.home_color, prepData.homeColor) {
						changeTeam = true
						changeTeamColor = true
					}
					Setting(R.string.home_players, "0") {}
				}
				Column (modifier = Modifier.weight(1f)){
					Setting(R.string.away_name, prepData.awayName, 2) {
						changeTeam = false
						changeTeamName = true
					}
					Setting(R.string.away_color, prepData.awayColor, 2) {
						changeTeam = false
						changeTeamColor = true
					}
					Setting(R.string.away_players, "0", 2) {}
				}
			}
		}
		item {
			Setting(R.string.match_type, matchType.name, 1) {
				changeMatchType = true
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
					onClick = { saveMatchType = true }
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
						changeValue = true
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
						changeValue = true
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
						changeValue = true
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
						changeValue = true
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
						changeValue = true
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
						changeValue = true
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
						changeValue = true
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
						changeValue = true
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
						changeValue = true
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
	if (changeTeamName) {
		var newName by remember {
			mutableStateOf(if(changeTeam) prepData.homeName else prepData.awayName)
		}
		AlertDialog(
			title = { Text(if(changeTeam) R.string.home_name else R.string.away_name) },
			text = {
				TextField(
					onValueChange = { newName = it },
					value = newName,
					singleLine = true
				)
			},
			confirmButton = {
				TextButton(
					onClick = {
						if(changeTeam) prepData.homeName = newName
						else prepData.awayName = newName
						prepData.manualUpdate = true
						changeTeamName = false
					}
				) { Text(R.string.save) }
			},
			onDismissRequest = { changeTeamName = false },
			dismissButton = {
				TextButton(
					onClick = { changeTeamName = false }
				) { Text(R.string.cancel) }
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
							if(changeTeam) prepData.homeColor = color
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
	if (saveMatchType) {
		var name by remember { mutableStateOf("") }
		if(!standardMatchTypes.contains(matchType.name)) name = matchType.name
		AlertDialog(
			title = { Text(R.string.save_match_type) },
			text = {
				TextField(
					onValueChange = { name = it },
					value = name,
					singleLine = true,
					placeholder = { Text(R.string.enter_name) }
				)
			},
			confirmButton = {
				TextButton(
					onClick = {
						if(name.isEmpty()) {
							context.toast(R.string.fail_empty_name)
						} else if(standardMatchTypes.contains(name)){
							context.toast(R.string.fail_standard_match_type)
						} else {
							onSaveMatchType(name)
							saveMatchType = false
						}
					}
				) { Text(R.string.save) }
			},
			onDismissRequest = { saveMatchType = false },
			dismissButton = {
				TextButton(
					onClick = { saveMatchType = false }
				) { Text(R.string.cancel) }
			}
		)
	}
	if (changeValue) {
		AlertDialog(
			title = {
				Text(
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
					}
				)
			},
			text = {
				IntField(
					onValueChange = { changeValueValue = it },
					value = changeValueValue,
					canBe0 = changeValueCanBe0
				)
			},
			confirmButton = {
				TextButton(
					onClick = {
						if(changeValueCanBe0 || changeValueValue > 0) {
							when (changeValueType) {
								ValueTypes.PeriodTime -> matchType.periodTime = changeValueValue
								ValueTypes.PeriodCount -> matchType.periodCount = changeValueValue
								ValueTypes.Sinbin -> matchType.sinbin = changeValueValue
								ValueTypes.PointsTry -> matchType.pointsTry = changeValueValue
								ValueTypes.PointsCon -> matchType.pointsCon = changeValueValue
								ValueTypes.PointsGoal -> matchType.pointsGoal = changeValueValue
								ValueTypes.ClockPK -> matchType.clockPK = changeValueValue
								ValueTypes.ClockCon -> matchType.clockCon = changeValueValue
								ValueTypes.ClockRestart -> matchType.clockRestart = changeValueValue
								else -> {}
							}
							prepData.manualUpdate = true
						}
						changeValue = false
					}
				) { Text(R.string.save) }
			},
			onDismissRequest = { changeValue = false },
			dismissButton = {
				TextButton(
					onClick = { changeValue = false }
				) { Text(R.string.cancel) }
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