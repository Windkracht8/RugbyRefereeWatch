/*
 * Copyright 2020-2026 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.windkracht8.rugbyrefereewatch

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource

@Composable
fun StringInput(
	show: MutableState<Boolean>,
	title: Int? = null,
	value: String,
	onSave: (String) -> Unit
){
	var newValue by remember { mutableStateOf(value) }
	AlertDialog(
		title = { if(title != null) Text(stringResource(title)) },
		text = {
			TextField(
				onValueChange = { newValue = it },
				value = newValue,
				singleLine = true
			)
		},
		confirmButton = {
			TextButton(
				onClick = {
					onSave(newValue)
					show.value = false
				}
			) { Text(R.string.save) }
		},
		onDismissRequest = { show.value = false },
		dismissButton = {
			TextButton(
				onClick = { show.value = false }
			) { Text(R.string.cancel) }
		}
	)
}

@Composable
fun IntInput(
	show: MutableState<Boolean>,
	title: Int? = null,
	value: Int,
	canBe0: Boolean = true,
	onSave: (Int) -> Unit
){
	var newValue by remember { mutableIntStateOf(value) }
	AlertDialog(
		title = { if(title != null) Text(stringResource(title)) },
		text = {
			IntField(
				onValueChange = { newValue = it },
				value = newValue,
				canBe0 = canBe0
			)
		},
		confirmButton = {
			TextButton(
				onClick = {
					onSave(newValue)
					show.value = false
				}
			) { Text(R.string.save) }
		},
		onDismissRequest = { show.value = false },
		dismissButton = {
			TextButton(
				onClick = { show.value = false }
			) { Text(R.string.cancel) }
		}
	)
}
