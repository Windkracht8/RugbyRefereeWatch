/*
 * Copyright 2020-2025 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.windkracht8.rugbyrefereewatch

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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

	val teamColors = stringArrayResource(R.array.team_colors)
	val standardMatchTypes = stringArrayResource(R.array.match_types)
	val matchTypeNames = standardMatchTypes + MatchStore.customMatchTypeNames

	val labelWidthMatch = measureWidth(R.string.home_name) + 10.dp
	val labelWidthMatchType = measureWidth(R.string.clock_con) + 10.dp
	val labelWidthWatch = measureWidth(R.string.clock_con) + 10.dp

	LazyColumn(Modifier.fillMaxSize()) {
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
		rowLabelContent(R.string.home_name, labelWidthMatch, {
			TextField(
				modifier = Modifier.fillMaxWidth(),
				value = prepData.homeName,
				onValueChange = { prepData.homeName(it) },
				placeholder = { Text(R.string.home_name_hint) }
			)
		})
		rowLabelContent(R.string.home_color, labelWidthMatch, {
			Spinner(
				modifier = Modifier.fillMaxWidth(),
				options = teamColors,
				value = prepData.homeColor,
				onSelected = { prepData.homeColor(it) }
			)
		})
		rowLabelContent(R.string.away_name, labelWidthMatch, {
			TextField(
				modifier = Modifier.fillMaxWidth(),
				value = prepData.awayName,
				onValueChange = { prepData.awayName(it) },
				placeholder = { Text(R.string.away_name_hint) }
			)
		})
		rowLabelContent(R.string.away_color, labelWidthMatch, {
			Spinner(
				modifier = Modifier.fillMaxWidth(),
				options = teamColors,
				value = prepData.awayColor,
				onSelected = { prepData.awayColor(it) }
			)
		})
		rowLabelContent(R.string.match_type, labelWidthMatch, {
			Spinner(
				modifier = Modifier.fillMaxWidth(),
				options = matchTypeNames,
				value = matchType.name,
				onSelected = {
					matchType.name = it
					matchType.updateFields()
					prepData.manualUpdate = true
				}
			)
		})
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
			rowLabelContent(R.string.period_time, labelWidthMatchType, {
				IntField(
					value = matchType.periodTime,
					onValueChange = {
						matchType.periodTime = it
						prepData.manualUpdate = true
					},
					canBe0 = false
				)
			})
			rowLabelContent(R.string.period_count, labelWidthMatchType, {
				IntField(
					value = matchType.periodCount,
					onValueChange = {
						matchType.periodCount = it
						prepData.manualUpdate = true
					},
					canBe0 = false
				)
			})
			rowLabelContent(R.string.sinbin, labelWidthMatchType, {
				IntField(
					value = matchType.sinbin,
					onValueChange = {
						matchType.sinbin = it
						prepData.manualUpdate = true
					},
					canBe0 = false
				)
			})
			rowLabelContent(R.string.points_try, labelWidthMatchType, {
				IntField(
					value = matchType.pointsTry,
					onValueChange = {
						matchType.pointsTry = it
						prepData.manualUpdate = true
					},
					canBe0 = false
				)
			})
			rowLabelContent(R.string.points_con, labelWidthMatchType, {
				IntField(
					value = matchType.pointsCon,
					onValueChange = {
						matchType.pointsCon = it
						prepData.manualUpdate = true
					},
					canBe0 = true
				)
			})
			rowLabelContent(R.string.points_goal, labelWidthMatchType, {
				IntField(
					value = matchType.pointsGoal,
					onValueChange = {
						matchType.pointsGoal = it
						prepData.manualUpdate = true
					},
					canBe0 = true
				)
			})
			rowLabelContent(R.string.clock_pk, labelWidthMatchType, {
				IntField(
					value = matchType.clockPK,
					onValueChange = {
						matchType.clockPK = it
						prepData.manualUpdate = true
					},
					canBe0 = true
				)
			})
			rowLabelContent(R.string.clock_con, labelWidthMatchType, {
				IntField(
					value = matchType.clockCon,
					onValueChange = {
						matchType.clockCon = it
						prepData.manualUpdate = true
					},
					canBe0 = true
				)
			})
			rowLabelContent(R.string.clock_restart, labelWidthMatchType, {
				IntField(
					value = matchType.clockRestart,
					onValueChange = {
						matchType.clockRestart = it
						prepData.manualUpdate = true
					},
					canBe0 = true
				)
			})
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
			rowLabelContent(R.string.screen, labelWidthWatch, {
				TextButton(onClick = prepData::toggleKeepScreenOn) {
					Text(
						text = if (prepData.keepScreenOn) stringResource(R.string.keep_on)
						else stringResource(R.string.auto_off),
						color = colorScheme.onSurface
					)
				}
			})
			rowLabelContent(R.string.timer_type, labelWidthWatch, {
				TextButton(onClick = prepData::toggleTimerType) {
					Text(//TIMER_TYPE_UP = 0
						text =
							if (prepData.timerType) stringResource(R.string.timer_type_down)
							else stringResource(R.string.timer_type_up),
						color = colorScheme.onSurface
					)
				}
			})
			rowLabelContent(R.string.record_player, labelWidthWatch, {
				Checkbox(
					checked = prepData.recordPlayer,
					onCheckedChange = { prepData.toggleRecordPlayer() }
				)
			})
			rowLabelContent(R.string.record_pens, labelWidthWatch, {
				Checkbox(
					checked = prepData.recordPens,
					onCheckedChange = { prepData.toggleRecordPens() }
				)
			})
			rowLabelContent(R.string.delay_end, labelWidthWatch, {
				Checkbox(
					checked = prepData.delayEnd,
					onCheckedChange = { prepData.toggleDelayEnd() }
				)
			})
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
					placeholder = { Text(R.string.enter_name) }
				)
			},
			onDismissRequest = { saveMatchType = false },
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
			dismissButton = {
				TextButton(
					onClick = { saveMatchType = false }
				) { Text(R.string.cancel) }
			}
		)
	}
}

fun LazyListScope.rowLabelContent(label: Int, labelWidth: Dp, content: @Composable () -> Unit) {
	item {
		Row(
			modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			Text(
				modifier = Modifier.width(labelWidth),
				text = stringResource(label)
			)
			content()
		}
	}
}

@Composable
fun measureWidth(label: Int): Dp =
	with(LocalDensity.current) {
		rememberTextMeasurer().measure(
			text = stringResource(label),
			style = LocalTextStyle.current
		).size.width.toDp()
	}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, apiLevel = 35)
@Composable
fun PreviewTabPrepare() {
	W8Theme { Surface { TabPrepare(
		commsBTStatus = Comms.Status.CONNECTED_BT,
		MatchType("15s"), PrepData(),
		{}, {}, {}
	) } }
}
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, apiLevel = 35)
@Composable
fun PreviewTabPrepareDay() {
	W8Theme { Surface { TabPrepare(
		commsBTStatus = Comms.Status.CONNECTED_BT,
		MatchType("10s"), PrepData(),
		{}, {}, {}
	) } }
}
