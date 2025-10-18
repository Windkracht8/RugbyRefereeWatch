/*
 * Copyright 2020-2025 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.windkracht8.rugbyrefereewatch

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.garmin.android.connectiq.IQDevice
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission") //Handled by Permissions
class DeviceSelect : ComponentActivity() {
	var showNewBTDevices by mutableStateOf(false)
	var showNewIQDevices by mutableStateOf(false)
	var bondedBTDevices: Set<BluetoothDevice>? = null
	var bondedIQDevices: List<IQDevice>? = null
	public override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if (!Permissions.hasBT) finishAndRemoveTask()

		lifecycleScope.launch {
			Comms.status.collect { status ->
				when (status) {
					Comms.Status.CONNECTING ->
						startActivity(Intent(this@DeviceSelect, DeviceConnect::class.java))
					Comms.Status.CONNECTED_BT, Comms.Status.CONNECTED_IQ, Comms.Status.ERROR ->
						finishAndRemoveTask()
					Comms.Status.STARTING, Comms.Status.DISCONNECTED, null -> {}
				}
			}
		}

		enableEdgeToEdge()
		setContent {
			W8Theme {
				Surface {
					DeviceSelectScreen(
						onBTDeviceClick = ::onBTDeviceClick,
						onIQDeviceClick = ::onIQDeviceClick,
						onNewBTDeviceClick = ::onNewBTDeviceClick,
						onNewIQDeviceClick = ::onNewIQDeviceClick,
						showNewBTDevices = showNewBTDevices,
						showNewIQDevices = showNewIQDevices,
						bondedBTDevices = bondedBTDevices,
						bondedIQDevices = bondedIQDevices
					)
				}
			}
		}
	}
	fun onBTDeviceClick(device: BluetoothDevice) {
		logD{"onBTDeviceClick: ${device.name}"}
		runInBackground { Comms.connectBTDevice(device) }
	}
	fun onIQDeviceClick(device: IQDevice) {
		logD{"onIQDeviceClick: ${device.friendlyName}"}
		runInBackground { Comms.connectIQDevice(device) }
	}
	fun onNewBTDeviceClick() {
		logD{"onNewBTDeviceClick"}
		runInBackground {
			bondedBTDevices = Comms.getBondedBTDevices()
			showNewBTDevices = true
		}
	}
	fun onNewIQDeviceClick() {
		logD{"onNewIQDeviceClick"}
		bondedIQDevices = Comms.getBondedIQDevices()
		showNewIQDevices = true
	}
}

@SuppressLint("MissingPermission") //Handled by Permissions
@Composable
fun DeviceSelectScreen(
	onBTDeviceClick: (BluetoothDevice) -> Unit,
	onIQDeviceClick: (IQDevice) -> Unit,
	onNewBTDeviceClick: () -> Unit,
	onNewIQDeviceClick: () -> Unit,
	showNewBTDevices: Boolean,
	showNewIQDevices: Boolean,
	bondedBTDevices: Set<BluetoothDevice>?,
	bondedIQDevices: List<IQDevice>?
) {
	val longPressTimeoutMillis = LocalViewConfiguration.current.longPressTimeoutMillis
	var confirmDelDevice by remember { mutableStateOf(null as Any?) }
	var showNewWatch by remember { mutableStateOf(true) }
	LazyColumn(Modifier.fillMaxSize().safeDrawingPadding()) {
		item {
			Text(
				modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
				text = stringResource(R.string.device_select_title),
				fontSize = 20.sp,
				textAlign = TextAlign.Center
			)
		}
		items(Comms.knownBTDevices.toList()) { device ->
			var isShort = true
			val interactionSource = remember { MutableInteractionSource() }
			LaunchedEffect(interactionSource) {
				interactionSource.interactions.collectLatest { interaction ->
					when (interaction) {
						is PressInteraction.Press -> {
							isShort = true
							delay(longPressTimeoutMillis)
							isShort = false
							confirmDelDevice = device.address
						}
						is PressInteraction.Release -> if (isShort) onBTDeviceClick(device)
					}
				}
			}
			Button(
				modifier = Modifier.fillMaxWidth().height(60.dp).padding(10.dp),
				interactionSource = interactionSource,
				onClick = {}
			) { Text(device.name ?: "<no name>") }
		}
		items(Comms.knownIQDevices.toList()) { device ->
			var isShort = true
			val interactionSource = remember { MutableInteractionSource() }
			LaunchedEffect(interactionSource) {
				interactionSource.interactions.collectLatest { interaction ->
					when (interaction) {
						is PressInteraction.Press -> {
							isShort = true
							delay(longPressTimeoutMillis)
							isShort = false
							confirmDelDevice = device.deviceIdentifier
						}
						is PressInteraction.Release -> if (isShort) onIQDeviceClick(device)
					}
				}
			}
			Button(
				modifier = Modifier.fillMaxWidth().height(60.dp).padding(10.dp),
				interactionSource = interactionSource,
				onClick = {}
			) { Text(device.friendlyName ?: "<no name>") }
		}
		if (showNewWatch) {
			item {
				OutlinedButton(
					modifier = Modifier.fillMaxWidth().height(60.dp).padding(10.dp),
					onClick = {
						showNewWatch = false
						onNewBTDeviceClick()
					}
				) { Text(R.string.device_select_new) }
			}
			item {
				OutlinedButton(
					modifier = Modifier.fillMaxWidth().height(60.dp).padding(10.dp),
					onClick = {
						showNewWatch = false
						onNewIQDeviceClick()
					}
				) { Text(R.string.device_select_garmin_new) }
			}
		}
		if (showNewBTDevices) {
			if (bondedBTDevices?.isEmpty() ?: true) {
				item { Text(R.string.device_select_none) }
			}
			if(bondedBTDevices != null){
				items(bondedBTDevices.toList()) { device ->
					OutlinedButton(
						modifier = Modifier.fillMaxWidth().height(60.dp).padding(10.dp),
						onClick = { onBTDeviceClick(device) }
					) { Text(device.name ?: "<no name>") }
				}
			}
		}
		if (showNewIQDevices) {
			if (bondedIQDevices?.isEmpty() ?: true) {
				item { Text(when(Comms.iQSdkStatus) {
						Comms.IQSdkStatus.GCM_NOT_INSTALLED -> R.string.device_select_garmin_not
						Comms.IQSdkStatus.GCM_UPGRADE_NEEDED -> R.string.device_select_garmin_update
						else -> R.string.device_select_garmin_none
				} ) }
			}
			if(bondedIQDevices != null){
				items(bondedIQDevices.toList()) { device ->
					OutlinedButton(
						modifier = Modifier.fillMaxWidth().height(60.dp).padding(10.dp),
						onClick = { onIQDeviceClick(device) }
					) { Text(device.friendlyName ?: "<no name>") }
				}
			}
		}
		if (confirmDelDevice != null) {
			item {
				AlertDialog(
					title = { Text(R.string.delete_device) },
					onDismissRequest = { confirmDelDevice = null },
					confirmButton = {
						TextButton(
							onClick = {
								if(confirmDelDevice is Long) Comms.delKnownIQId(confirmDelDevice as Long)
								else Comms.delKnownBTAddress(confirmDelDevice as String)
								confirmDelDevice = null
							}
						) { Text(R.string.delete) }
					},
					dismissButton = {
						TextButton(
							onClick = { confirmDelDevice = null }
						) { Text(R.string.cancel) }
					}
				)
			}
		}
	}
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, apiLevel = 35)
@Composable
fun PreviewDeviceSelect() {
	Comms
	W8Theme { Surface { DeviceSelectScreen(
		{}, {}, {}, {},
		showNewBTDevices = false, showNewIQDevices = false, bondedBTDevices = null, bondedIQDevices = null
	) } }
}
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, apiLevel = 35)
@Composable
fun PreviewDeviceSelectDay() {
	W8Theme { Surface { DeviceSelectScreen(
		{}, {}, {}, {},
		showNewBTDevices = false, showNewIQDevices = false, bondedBTDevices = null, bondedIQDevices = null
	) } }
}
