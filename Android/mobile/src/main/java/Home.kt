/*
 * Copyright 2020-2025 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.windkracht8.rugbyrefereewatch

import android.content.res.Configuration
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

const val TAB_INDEX_HISTORY = 0
const val TAB_INDEX_REPORT = 1
const val TAB_INDEX_PREPARE = 2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Home(
	commsBTStatus: Comms.Status?,
	onIconClick: () -> Unit,
	onImportClick: () -> Unit,
	deleteMatches: (Set<Long>) -> Unit,
	exportMatches: (Set<Long>) -> Unit,
	shareMatch: (Long, List<Boolean>) -> Unit,
	saveMatch: (MatchData) -> Unit,
	matchType: MatchType,
	prepData: PrepData,
	onPrepareClicked: () -> Unit,
	onSaveMatchType: (String) -> Unit,
	onDeleteMatchType: () -> Unit
){
	val iconWatchConnecting =
		AnimatedImageVector.animatedVectorResource(R.drawable.icon_watch_connecting)
	var iconWatchConnectingAtEnd by remember { mutableStateOf(false) }
	val navController = rememberNavController()
	var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
	LaunchedEffect(navController) {
		navController.addOnDestinationChangedListener { c, destination, a ->
			when (destination.route) {
				"history" -> selectedTabIndex = TAB_INDEX_HISTORY
				"report/{matchId}" -> selectedTabIndex = TAB_INDEX_REPORT
				"prepare" -> selectedTabIndex = TAB_INDEX_PREPARE
			}
		}
	}
	Column(Modifier.fillMaxSize().safeDrawingPadding()) {
		Row(Modifier.fillMaxWidth().height(70.dp)) {
			Box(
				modifier = Modifier.size(70.dp),
				contentAlignment = Alignment.Center
			) {
				if (commsBTStatus in listOf(Comms.Status.CONNECTING, Comms.Status.STARTING)) {
					Image(
						modifier = Modifier.size(70.dp).clickable { onIconClick() },
						painter = rememberAnimatedVectorPainter(
							iconWatchConnecting,
							iconWatchConnectingAtEnd
						),
						contentDescription = "watch icon"
					)
					iconWatchConnectingAtEnd = true
				} else {
					Icon(
						modifier = Modifier.size(70.dp).clickable { onIconClick() },
						imageVector = ImageVector.vectorResource(R.drawable.icon_watch),
						tint = when (commsBTStatus) {
							Comms.Status.DISCONNECTED, null -> colorScheme.onBackground.copy(alpha = 0.38f)
							Comms.Status.ERROR -> colorScheme.error
							else -> colorScheme.onBackground
						},
						contentDescription = "watch icon"
					)
				}
			}
			Column(Modifier.fillMaxWidth()) {
				Text(
					modifier = Modifier.fillMaxWidth(),
					text = when (commsBTStatus) {
						Comms.Status.DISCONNECTED ->
							stringResource(R.string.disconnected)
						Comms.Status.CONNECTING ->
							stringResource(R.string.connecting_to, Comms.deviceName)
						Comms.Status.CONNECTED_BT, Comms.Status.CONNECTED_IQ ->
							stringResource(R.string.connected_to, Comms.deviceName)
						Comms.Status.STARTING, null ->
							if (Permissions.hasBT) stringResource(R.string.starting)
							else stringResource(R.string.no_permission)
						Comms.Status.ERROR -> stringResource(Comms.error)
					},
					fontSize = 18.sp
				)
				Text(
					modifier = Modifier.fillMaxWidth(),
					text = if (Comms.messageStatus <= 0) ""
						else stringResource(Comms.messageStatus),
					fontSize = 14.sp
				)
			}
		}
		PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
			Tab(
				selected = selectedTabIndex == TAB_INDEX_HISTORY,
				onClick = { navController.navigate("history") },
				text = { Text(R.string.history) }
			)
			Tab(
				selected = selectedTabIndex == TAB_INDEX_REPORT,
				onClick = { navController.navigate("report/null") },
				text = { Text(R.string.report) }
			)
			Tab(
				selected = selectedTabIndex == TAB_INDEX_PREPARE,
				onClick = { navController.navigate("prepare") },
				text = { Text(R.string.prepare) }
			)
		}
		NavHost(
			navController = navController,
			startDestination = "history"
		){
			composable("history") {
				TabHistory(
					onMatchClick = { navController.navigate("report/$it") },
					onImportClick = onImportClick,
					deleteMatches = deleteMatches,
					exportMatches = exportMatches
				)
			}
			composable("report/{matchId}") {
				TabReport(
					matchId = it.arguments?.getString("matchId")?.toLongOrNull(),
					shareMatch = shareMatch,
					saveMatch = saveMatch
				)
			}
			composable("prepare") {
				TabPrepare(
					commsBTStatus = commsBTStatus,
					matchType = matchType,
					prepData = prepData,
					onPrepareClicked = onPrepareClicked,
					onSaveMatchType = onSaveMatchType,
					onDeleteMatchType = onDeleteMatchType
				)
			}
		}
	}
}

@Composable
fun Text(text: Int) = Text(stringResource(text))

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, apiLevel = 35)
@Composable
fun PreviewHome() {
	Comms.deviceName = "Test"
	W8Theme { Surface {
		Home(
			commsBTStatus = Comms.Status.CONNECTING,
			{}, {}, {}, {},
			shareMatch = {_: Long, _: List<Boolean> ->}, {},
			MatchType("15s"), PrepData(),
			{}, {}, {}
		)
	} }
}
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, apiLevel = 35)
@Composable
fun PreviewHomeDay() {
	W8Theme { Surface {
		Home(
			commsBTStatus = Comms.Status.CONNECTING,
			{}, {}, {}, {},
			shareMatch = {_: Long, _: List<Boolean> ->}, {},
			MatchType("15s"), PrepData(),
			{}, {}, {}
		)
	} }
}
