/*
 * Copyright 2020-2026 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.windkracht8.rugbyrefereewatch

import android.Manifest.permission.BLUETOOTH
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat.checkSelfPermission

class Permissions : ComponentActivity() {
	companion object {
		var hasBT by mutableStateOf(false)
		fun checkPermissions(context: Context) {
			hasBT = if (Build.VERSION.SDK_INT >= 31) {
				context.hasPermission(BLUETOOTH_CONNECT)
			} else {
				context.hasPermission(BLUETOOTH)
			}
		}
	}
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if(hasBT) finishAndRemoveTask()
		setContent { W8Theme (window, resources) { Surface { PermissionsScreen(::onNearbyClick) } } }
	}
	fun onNearbyClick() {
		if(hasBT) return
		if(Build.VERSION.SDK_INT >= 31) { requestPermissionBT.launch(BLUETOOTH_CONNECT)}
		else { requestPermissionBT.launch(BLUETOOTH) }
	}
	val requestPermissionBT = registerForActivityResult(
		ActivityResultContracts.RequestPermission()){
		hasBT = it
		if(hasBT) finishAndRemoveTask()
	}
}

fun Context.hasPermission(permission: String): Boolean =
	checkSelfPermission(this, permission) == PERMISSION_GRANTED

@Composable
fun PermissionsScreen(onNearbyClick: () -> Unit) {
	Column(Modifier.fillMaxWidth().safeDrawingPadding()) {
		Text(
			modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
			text = stringResource(R.string.permission_title),
			color = colorScheme.primary,
			fontSize = 20.sp,
			fontWeight = FontWeight.Bold,
			textAlign = TextAlign.Center
		)
		Text(
			modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
			text = stringResource(R.string.permission_nearby_title),
			fontSize = 20.sp,
			textAlign = TextAlign.Center
		)
		Button(
			modifier = Modifier.fillMaxWidth(),
			onClick = onNearbyClick
		) { Text(R.string.permission_nearby) }
		HorizontalDivider(
			modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
			thickness = 2.dp,
		)
		Text(
			modifier = Modifier.fillMaxWidth(),
			text = stringResource(R.string.permission_explain),
			fontSize = 14.sp,
			textAlign = TextAlign.Center
		)
	}
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.PIXEL_8)
@Composable
fun PreviewPermissions() { W8Theme (null, null) { Surface { PermissionsScreen {} } } }
@Preview(device = Devices.PIXEL_8)
@Composable
fun PreviewPermissionsDay() { PreviewPermissions() }
