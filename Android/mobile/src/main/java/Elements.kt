/*
 * Copyright 2020-2026 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.windkracht8.rugbyrefereewatch

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType

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

@Composable
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
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
