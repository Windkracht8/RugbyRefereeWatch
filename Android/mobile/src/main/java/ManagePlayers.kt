/*
 * Copyright 2020-2026 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.windkracht8.rugbyrefereewatch

import android.content.Context.MODE_PRIVATE
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.windkracht8.rugbyrefereewatch.MatchData.Player
import kotlinx.coroutines.launch

@Composable
fun ManagePlayers(
	prepData: PrepData,
	isHome: Boolean,
	close: () -> Unit
){
	val coroutineScope = rememberCoroutineScope()
	val listState = rememberLazyListState()
	val tempPlayers = remember { mutableStateListOf<Player>() }
	if (isHome) prepData.homePlayers.forEach { tempPlayers.add(Player(it)) }
	else prepData.awayPlayers.forEach { tempPlayers.add(Player(it)) }
	tempPlayers.sort()
	if(tempPlayers.isEmpty()) for (i in 1..23) tempPlayers.add(Player(i))

	Column(Modifier.fillMaxSize().background(colorScheme.background).padding(horizontal = 5.dp)) {
		Text(
			modifier = Modifier.fillMaxWidth(),
			text = stringResource(if(isHome) R.string.home_players else R.string.away_players),
			textAlign = TextAlign.Center
		)
		Row {
			OutlinedButton(
				onClick = {
					val number =
						if (tempPlayers.isEmpty()) 1
						else tempPlayers.maxOf { it.number } + 1
                    tempPlayers.add(Player(number))
					coroutineScope.launch { listState.scrollToItem(tempPlayers.size - 1) }
				}
			) { Text(R.string.add_player) }
			Spacer(Modifier.weight(1f))
			OutlinedButton(
				onClick = {
                    tempPlayers.clear()
					for (i in 1..23) { tempPlayers.add(Player(i)) }
				}
			) { Text(R.string.reset) }
		}
		LazyColumn(
			modifier = Modifier.weight(1f),
			state = listState
		) {
			items(tempPlayers){ player -> ManagePlayersPlayer(player, tempPlayers) }
		}
		Row {
			OutlinedButton(
				onClick = close
			) { Text(R.string.cancel) }
			Spacer(Modifier.weight(1f))
			Button(
				onClick = {
                    if (isHome) prepData.homePlayers = tempPlayers
                    else prepData.awayPlayers = tempPlayers
					close()
                }
			) { Text(R.string.save) }
		}
	}

	val sharedPreferences = LocalContext.current.getSharedPreferences("ManagePlayers", MODE_PRIVATE)
	var showManagePlayersIntro by remember { mutableStateOf(
		sharedPreferences.getBoolean("ManagePlayersIntro", true)
	) }
	if (showManagePlayersIntro) {
		AlertDialog(
			text = {
				Text(
					text = stringResource(R.string.manage_players_intro),
					textAlign = TextAlign.Center
				)
			},
			confirmButton = {
				TextButton(
					onClick = { showManagePlayersIntro = false }
				) { Text(R.string.got_it) }
			},
			onDismissRequest = { showManagePlayersIntro = false },
		)
		sharedPreferences.edit { putBoolean("ManagePlayersIntro", false) }
	}
}
@Composable
fun ManagePlayersPlayer(
	player: Player,
	tempPlayers: MutableList<Player>
){
	val changeNumber = remember { mutableStateOf(false) }
	val changeName = remember { mutableStateOf(false) }
	var changePlayer by remember { mutableStateOf<Player?>(null) }

	Row(modifier = Modifier.fillMaxWidth()) {
		TextButton(
			onClick = { changePlayer = player; changeNumber.value = true }
		){
			Text(
				text = player.number.toString(),
				color = colorScheme.onSurface
			)
		}
		TextButton(
			modifier = Modifier.weight(1f),
			onClick = { changePlayer = player; changeName.value = true }
		){
			Text(
				modifier = Modifier.fillMaxWidth(),
				text = player.name.ifEmpty { stringResource(R.string.no_name) },
				color = colorScheme.onSurface
			)
		}
		TextButton(
			onClick = { player.frontRow = !player.frontRow }
		){
			Text(
				text = stringResource(R.string.front_row),
				color = if(player.frontRow) colorScheme.primary else colorScheme.onSurface,
				fontSize = 10.sp
			)
		}
		TextButton(
			onClick = {
				player.captain = !player.captain
				tempPlayers.forEach { if(it != player) it.captain = false }
			}
		){
			Text(
				text = stringResource(R.string.captain),
				color = if(player.captain) colorScheme.primary else colorScheme.onSurface,
				fontSize = 10.sp
			)
		}

		IconButton(
			modifier = Modifier.size(48.dp),
			onClick = { tempPlayers.remove(player) }
		) {
			Icon(
				imageVector = Icons.Default.Delete,
				contentDescription = stringResource(R.string.delete)
			)
		}
	}
	if (changeNumber.value) {
		val context = LocalContext.current
		IntInput(
			show = changeNumber,
			title = R.string.enter_player_number,
			value = changePlayer?.number ?: 0,
			onSave = { number ->
				if(tempPlayers.any { it.number == number }) context.toast(R.string.player_number_exists)
				else changePlayer?.number = number
			}
		)
	}
	if (changeName.value) {
		StringInput(
			show = changeName,
			title = R.string.enter_player_name,
			value = changePlayer?.name ?: "",
			onSave = { changePlayer?.name = it }
		)
	}
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewManagePlayers() {
	val prepData = PrepData()
	val tempPlayers = mutableListOf<Player>()
	for (i in 1..23) {
		val player = Player(i)
		player.frontRow = i < 4
		player.captain = i == 7
		tempPlayers.add(player)
	}
	prepData.homePlayers = tempPlayers
	W8Theme (null, null) { Surface { ManagePlayers(
		prepData,
		isHome = true,
		close = {}
	) } }
}
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun PreviewManagePlayersDay() { PreviewManagePlayers() }
