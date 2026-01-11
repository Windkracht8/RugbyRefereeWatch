/*
 * Copyright 2020-2025 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.windkracht8.rugbyrefereewatch

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TabHistory(
	onMatchClick: (Long) -> Unit,
	onImportClick: () -> Unit,
	deleteMatches: (Set<Long>) -> Unit,
	exportMatches: (Set<Long>) -> Unit
){
	val context = LocalContext.current
	val matches = remember(MatchStore.matches) { MatchStore.matches }
	var selected by remember { mutableStateOf(setOf<Long>()) }
	var confirmDel by remember { mutableStateOf(false) }
	Column(Modifier.fillMaxSize()) {
		LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
			items(matches.reversed()) { match ->
				HorizontalDivider()
				Text(
					modifier = Modifier
						.fillMaxWidth()
						.padding(vertical = 5.dp)
						.background(
							color = if (selected.contains(match.matchId)) {
								colorScheme.surfaceVariant
							} else {
								colorScheme.surface
							}
						)
						.combinedClickable(
							true,
							onClick = {
								if (selected.isNotEmpty()) {
									selected = if (selected.contains(match.matchId)) {
										selected - match.matchId
									} else {
										selected + match.matchId
									}
								} else {
									onMatchClick(match.matchId)
								}
							},
							onLongClick = {
								selected = if (selected.contains(match.matchId)) {
									selected - match.matchId
								} else {
									selected + match.matchId
								}
							}),
					text = matchTitle(match),
					maxLines = 1,
					overflow = TextOverflow.Ellipsis
				)
			}
		}
		Row {
			TextButton(
				modifier = Modifier.weight(1f),
				onClick = onImportClick
			) { Text(R.string._import) }
			TextButton(
				modifier = Modifier.weight(1f),
				onClick = {
					if(selected.isEmpty()) context.toast(R.string.select_match)
					else confirmDel = true
				}
			) {
				Text(
					text = stringResource(R.string.delete).lowercase(),
					color = if(selected.isNotEmpty()) colorScheme.error
							else colorScheme.error.copy(alpha = 0.5f)
				)
			}
			TextButton(
				modifier = Modifier.weight(1f),
				onClick = {
					exportMatches(selected)
					selected = setOf()
				}
			) { Text(R.string.export) }
		}
	}
	if (confirmDel) {
		AlertDialog(
			title = { Text(R.string.delete_matches) },
			onDismissRequest = {
				selected = setOf()
				confirmDel = false
			},
			confirmButton = {
				TextButton(
					onClick = {
						deleteMatches(selected)
						selected = setOf()
						confirmDel = false
					}
				) { Text(R.string.delete) }
			},
			dismissButton = {
				TextButton(
					onClick = {
						selected = setOf()
						confirmDel = false
					}
				) { Text(R.string.cancel) }
			}
		)
	}
}

fun matchTitle(match: MatchData): String {
	val date = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())
					.format(Date(match.matchId))
	return "$date ${match.home.team} v ${match.away.team} ${match.home.tot}:${match.away.tot}"
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, apiLevel = 35)
@Composable
fun PreviewTabHistory() {
	MatchStore.matches.add(MatchData(JSONObject("{\"matchid\":1741956291022,\"format\":3,\"settings\":{\"match_type\":\"7s\",\"period_time\":7,\"period_count\":2,\"sinbin\":2,\"points_try\":5,\"points_con\":2,\"points_goal\":3,\"pk_clock\":30,\"conv_clock\":30,\"restart_clock\":30},\"home\":{\"id\":\"home\",\"team\":\"RSA\",\"color\":\"green\",\"tot\":26,\"tries\":4,\"cons\":3,\"pen_tries\":0,\"goals\":0,\"yellow_cards\":1,\"red_cards\":0,\"pens\":0,\"kickoff\":true},\"away\":{\"id\":\"away\",\"team\":\"FRA\",\"color\":\"blue\",\"tot\":14,\"tries\":2,\"cons\":2,\"pen_tries\":0,\"goals\":0,\"yellow_cards\":0,\"red_cards\":0,\"pens\":0,\"kickoff\":false},\"events\":[]}")))
	MatchStore.matches.add(MatchData(JSONObject("{\"matchid\":1743850855028,\"format\":3,\"settings\":{\"match_type\":\"custom\",\"period_time\":25,\"period_count\":2,\"sinbin\":7,\"points_try\":5,\"points_con\":2,\"points_goal\":3,\"clock_pk\":60,\"clock_con\":60,\"clock_restart\":0},\"home\":{\"id\":\"home\",\"team\":\"U14 Donau\",\"color\":\"white\",\"tot\":56,\"tries\":8,\"cons\":8,\"pen_tries\":0,\"goals\":0,\"yellow_cards\":0,\"red_cards\":0,\"pens\":0,\"kickoff\":true,\"pen_goals\":0,\"drop_goals\":0},\"away\":{\"id\":\"away\",\"team\":\"Celtics\",\"color\":\"blue\",\"tot\":7,\"tries\":1,\"cons\":1,\"pen_tries\":0,\"goals\":0,\"yellow_cards\":1,\"red_cards\":0,\"pens\":0,\"kickoff\":false,\"pen_goals\":0,\"drop_goals\":0},\"events\":[]}")))
	W8Theme (null, null) { Surface { TabHistory({},{},{},{}) } }
}
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, apiLevel = 35)
@Composable
fun PreviewTabHistoryDay() {
	W8Theme (null, null) { Surface { TabHistory({},{},{},{}) } }
}
