/*
 * Copyright 2020-2025 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.windkracht8.rugbyrefereewatch

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

@SuppressLint("MissingPermission")//handled by Permissions
object CommsBT {
	val RRW_UUID: UUID = UUID.fromString("8b16601b-5c76-4151-a930-2752849f4552")

	var sharedPreferences: SharedPreferences? = null
	var bluetoothAdapter: BluetoothAdapter? = null
	var bluetoothSocket: BluetoothSocket? = null
	var commsBTConnect: CommsBTConnect? = null
	var commsBTConnected: CommsBTConnected? = null
	val knownDevices: MutableSet<BluetoothDevice> = mutableSetOf()
	var knownAddresses: MutableSet<String> = mutableSetOf()

	enum class Status { STARTING, DISCONNECTED, CONNECTING, CONNECTED, ERROR }
	val status = MutableStateFlow(null as Status?)
	var error by mutableIntStateOf(-1)
	var messageStatus by mutableIntStateOf(-1)
	val messageError = MutableSharedFlow<Int>()
	val watchMatch = MutableSharedFlow<MatchData>()
	val watchSettings = MutableSharedFlow<JSONObject>()
	var deviceName = ""
	var disconnect = false
	val requestQueue: MutableSet<Request> = mutableSetOf()
	var lastRequest: Request? = null

	fun start(activity: Activity) {
		if (!Permissions.hasBT) return onError(R.string.fail_BT_denied)
		status.value = Status.STARTING

		val bm = activity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
		bluetoothAdapter = bm.adapter
		if (bluetoothAdapter?.state != BluetoothAdapter.STATE_ON) return onError(R.string.fail_BT_off)

		sharedPreferences = activity.applicationContext.getSharedPreferences("CommsBT", MODE_PRIVATE)
		knownAddresses = sharedPreferences?.getStringSet("knownAddresses", null) ?: mutableSetOf()
		logD("CommsBT.startBT " + knownAddresses.size + " known addresses")
		val bondedBTDevices = bluetoothAdapter?.bondedDevices ?: emptySet()
		//Find and clean known devices
		knownAddresses.forEach { checkKnownAddress(it, bondedBTDevices) }
		//Try to connect to known device
		if (knownDevices.isNotEmpty()) {
			connectDevice(knownDevices.first())
			return //having multiple watches is rare, user will have to select from DeviceSelect
		}
		status.value = Status.DISCONNECTED
	}

	fun checkKnownAddress(knownAddress: String, bondedDevices: Set<BluetoothDevice>) {
		for (device in bondedDevices) {
			if (device.address == knownAddress) {
				knownDevices.add(device)
				return
			}
		}
		delKnownAddress(knownAddress)
	}

	fun delKnownAddress(address: String) {
		if (knownAddresses.remove(address)) storeKnownAddresses()
	}

	fun storeKnownAddresses() {
		sharedPreferences?.edit {
			if (knownAddresses.isEmpty()) {
				remove("knownAddresses")
			} else {
				putStringSet("knownAddresses", knownAddresses)
			}
		}
	}

	fun stop() {
		disconnect = true
		tryIgnore { bluetoothSocket?.close() }
		bluetoothSocket = null
		commsBTConnect = null
		commsBTConnected = null
		status.value = Status.DISCONNECTED
		messageStatus = -1
	}

	fun getBondedDevices(): Set<BluetoothDevice>? {
		if (bluetoothAdapter == null) {
			onError(R.string.fail_BT_denied)
			return null
		}
		if (bluetoothAdapter?.state != BluetoothAdapter.STATE_ON) {
			onError(R.string.fail_BT_off)
			return null
		}
		return bluetoothAdapter?.bondedDevices
	}

	fun connectDevice(device: BluetoothDevice) {
		logD("CommsBT.connectDevice: " + device.name)
		if (status.value in listOf(Status.CONNECTED, Status.CONNECTING) ||
			bluetoothAdapter?.isEnabled != true
		) return
		disconnect = false
		deviceName = device.name ?: "<no name>"
		status.value = Status.CONNECTING
		commsBTConnect = CommsBTConnect(device)
		commsBTConnect?.start()
	}

	fun sendRequestPrep(settings: JSONObject) =
		requestQueue.add(Request(Request.Type.PREPARE, settings = settings))

	suspend fun gotResponse(response: JSONObject) {
		try {
			val requestType = response.getString("requestType")
			when (requestType) {
				"sync" -> {
					//{requestType":"sync","responseData":{"match_ids":[],"settings":{}}}
					val responseData = response.getJSONObject("responseData")
					val matchIdsJson = responseData.getJSONArray("match_ids")
					val matchIds: MutableSet<Long> = HashSet()
					for (i in 0..<matchIdsJson.length()) {
						matchIds.add(matchIdsJson.getLong(i))
					}
					gotMatchIds(matchIds)
					val settingsJson = responseData.getJSONObject("settings")
					watchSettings.emit(settingsJson)
					if(requestQueue.isEmpty()) messageStatus = R.string.sync_done
				}
				"getMatch" -> {
					//{"requestType":"getMatch","responseData":{ match object }}
					val matchJson = response.getJSONObject("responseData")
					watchMatch.emit(MatchData(matchJson))
					if(requestQueue.isEmpty()) messageStatus = R.string.sync_done
				}
				"delMatch" -> {
					//{"requestType":"delMatch","responseData":"okilly dokilly"}
					if(response.getString("responseData") != "okilly dokilly")
						logE("Failed to delete match")
					if(requestQueue.isEmpty()) messageStatus = R.string.sync_done
				}
				"prepare" -> {
					//{"requestType":"prepare","responseData":"okilly dokilly"}
					//{"requestType":"prepare","responseData":"match ongoing"}
					when (response.getString("responseData")) {
						"okilly dokilly" -> messageStatus = R.string.prep_done
						"match ongoing" -> onMessageError(R.string.match_ongoing)
						else -> throw Exception()
					}
				}
			}
		} catch (e: Exception) {
			logE("CommsBT.gotResponse: " + e.message)
			onMessageError(R.string.fail_response)
		}
		lastRequest = null
	}
	fun gotMatchIds(matchIds: Set<Long>) {
		matchIds.forEach { matchId ->
			if(MatchStore.matches.none { it.matchId == matchId }) {
				requestQueue.add(Request(Request.Type.GET_MATCH, matchId))
			}
			if(MatchStore.deletedMatches.contains(matchId)) {
				requestQueue.add(Request(Request.Type.DEL_MATCH, matchId))
			}
		}
		MatchStore.deletedMatches.forEach { matchId ->
			if(!matchIds.contains(matchId)) MatchStore.deletedMatches.remove(matchId)
		}
	}

	class CommsBTConnect(device: BluetoothDevice) : Thread() {
		init {
			logD("CommsBTConnect " + device.name)
			try {
				bluetoothSocket = device.createRfcommSocketToServiceRecord(RRW_UUID)
			} catch (e: Exception) {
				logE("CommsBTConnect Exception: " + e.message)
				status.value = Status.DISCONNECTED
			}
		}
		override fun run() {
			try {
				bluetoothSocket?.connect()
				commsBTConnected = CommsBTConnected()
				commsBTConnected?.start()
			} catch (e: Exception) {
				logD("CommsBTConnect.run failed: " + e.message)
				tryIgnore { bluetoothSocket?.close() }
				status.value = Status.DISCONNECTED
			}
		}
	}

	class CommsBTConnected : Thread() {
		var inputStream: InputStream? = null
		var outputStream: OutputStream? = null
		init {
			logD("CommsBTConnected")
			try {
				inputStream = bluetoothSocket!!.inputStream
				outputStream = bluetoothSocket!!.outputStream
				status.value = Status.CONNECTED
				knownDevices.add(bluetoothSocket!!.remoteDevice)
				if (knownAddresses.add(bluetoothSocket!!.remoteDevice.address)) {
					storeKnownAddresses()
				}
			} catch (e: Exception) {
				logE("CommsBTConnected init Exception: " + e.message)
				status.value = Status.DISCONNECTED
			}
		}
		override fun run() {
			requestQueue.add(Request(Request.Type.SYNC))
			runBlocking { process() }
		}
		fun close() {
			logD("CommsBTConnected.close")
			status.value = Status.DISCONNECTED
			messageStatus = -1
			tryIgnore {
				requestQueue.clear()
				bluetoothSocket?.close()
			}
			bluetoothSocket = null
			commsBTConnected = null
			commsBTConnect = null
		}
		suspend fun process() {
			while (!disconnect) {
				try {
					outputStream!!.write("".toByteArray())
				} catch (_: Exception) {
					logD("Connection closed")
					break
				}
				sendNextRequest()
				read()
				delay(100)
			}
			close()
		}
		fun sendNextRequest() {
			try {
				outputStream!!.write("".toByteArray())
				if (requestQueue.isEmpty() || lastRequest != null) return
				lastRequest = requestQueue.first()
				requestQueue.remove(lastRequest)
				logD("CommsBTConnected.sendNextRequest: $lastRequest")
				outputStream!!.write(lastRequest.toString().toByteArray())
			} catch (e: Exception) {
				logE("CommsBTConnected.sendNextRequest Exception: " + e.message)
				onMessageError(R.string.fail_send_message)
				disconnect = true
			}
		}
		suspend fun read() {
			try {
				if (inputStream!!.available() < 5) return
				var lastReadTime = System.currentTimeMillis()
				var response = ""
				while (System.currentTimeMillis() - lastReadTime < 3000) {
					if (inputStream!!.available() == 0) {
						delay(100)
						continue
					}
					val buffer = ByteArray(inputStream!!.available())
					val numBytes = inputStream!!.read(buffer)
					if (numBytes < 0) {
						logE("CommsBTConnected.read read error, response: $response")
						lastRequest = null
						return onMessageError(R.string.fail_response)
					} else if (numBytes > 0) {
						lastReadTime = System.currentTimeMillis()
					}
					val temp = String(buffer)
					response += temp
					if (isValidJSON(response)) {
						logD("CommsBTConnected.read got message: $response")
						gotResponse(JSONObject(response))
						return
					}
				}
				logE("CommsBTConnected.read no valid message and no new data after 3 sec: $response")
				lastRequest = null
			} catch (e: Exception) {
				logE("CommsBTConnected.read Exception: " + e.message)
				lastRequest = null
			}
			onMessageError(R.string.fail_response)
		}

		fun isValidJSON(json: String): Boolean {
			if (!json.endsWith("}")) return false
			try { JSONObject(json)} catch (_: JSONException) { return false }
			return true
		}
	}

	class Request(val type: Type, val matchId: Long? = null, val settings: JSONObject? = null) {
		enum class Type { SYNC, GET_MATCH, DEL_MATCH, PREPARE }
		override fun toString(): String {
			val request = JSONObject()
			when (type) {
				Type.SYNC -> {
					//{"requestType":"sync","requestData":{"custom_match_types":[]}}
					request.put("requestType", "sync")
					val requestData = JSONArray()
					MatchStore.customMatchTypes.forEach { requestData.put(it.toJson()) }
					request.put("requestData", requestData)
					messageStatus = R.string.sync
				}
				Type.GET_MATCH -> {
					//{"requestType":"getMatch","requestData":123456789}
					request.put("requestType", "getMatch")
					request.put("requestData", matchId)
					messageStatus = R.string.get_match
				}
				Type.DEL_MATCH -> {
					//{"requestType":"delMatch","requestData":123456789}
					request.put("requestType", "delMatch")
					request.put("requestData", matchId)
					messageStatus = R.string.del_match
				}
				Type.PREPARE -> {
					//{"requestType":"prepare","requestData":{ settings }}
					request.put("requestType", "prepare")
					request.put("requestData", settings)
					messageStatus = R.string.prep
				}
			}
			return request.toString()
		}
	}

	fun onError(message: Int) {
		error = message
		status.value = Status.ERROR
	}
	fun onMessageError(message: Int) {
		messageStatus = message
		runInBackground { messageError.emit(message) }
	}
}
