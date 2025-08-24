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
import android.content.pm.PackageManager
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.garmin.android.connectiq.ConnectIQ
import com.garmin.android.connectiq.ConnectIQ.ConnectIQListener
import com.garmin.android.connectiq.ConnectIQ.IQApplicationEventListener
import com.garmin.android.connectiq.ConnectIQ.IQApplicationInfoListener
import com.garmin.android.connectiq.ConnectIQ.IQDeviceEventListener
import com.garmin.android.connectiq.IQApp
import com.garmin.android.connectiq.IQDevice
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
import java.util.function.Consumer

@SuppressLint("MissingPermission")//handled by Permissions
object Comms: ConnectIQListener, IQApplicationEventListener, IQApplicationInfoListener, IQDeviceEventListener {
	const val EMULATOR_MODE = false//false = release; true = testing in emulators
	val RRW_UUID: UUID = UUID.fromString("8b16601b-5c76-4151-a930-2752849f4552")
	const val IQ_APP_ID: String = "A5E772CFCE6A4CF082133E2E8C52FFBA"

	var sharedPreferences: SharedPreferences? = null
	var bluetoothAdapter: BluetoothAdapter? = null
	var bluetoothSocket: BluetoothSocket? = null
	var commsBTConnect: CommsBTConnect? = null
	var commsBTConnected: CommsBTConnected? = null
	val knownBTDevices: MutableSet<BluetoothDevice> = mutableSetOf()
	var knownBTAddresses: MutableSet<String> = mutableSetOf()

	var iQApp: IQApp? = null
	var connectIQ: ConnectIQ? = null
	val knownIQIds: MutableSet<Long> = mutableSetOf()
	val knownIQDevices: MutableSet<IQDevice> = mutableSetOf()

	enum class IQSdkStatus { UNAVAILABLE, READY, GCM_NOT_INSTALLED, GCM_UPGRADE_NEEDED, ERROR }
	var iQSdkStatus: IQSdkStatus = IQSdkStatus.UNAVAILABLE
	private var iQDevice: IQDevice? = null

	enum class Status { STARTING, DISCONNECTED, CONNECTING, CONNECTED_BT, CONNECTED_IQ, ERROR }
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
		if (!(bluetoothAdapter?.isEnabled ?: false)) return onError(R.string.fail_BT_off)

		sharedPreferences = activity.applicationContext.getSharedPreferences("Comms", MODE_PRIVATE)
		knownBTAddresses = sharedPreferences?.getStringSet("knownBTAddresses", null) ?: mutableSetOf()
		logD("Comms.start " + knownBTAddresses.size + " known addresses")
		val bondedBTDevices = bluetoothAdapter?.bondedDevices ?: emptySet()
		//Find and clean known devices
		knownBTAddresses.forEach { checkKnownBTAddress(it, bondedBTDevices) }
		//Try to connect to known device
		if (knownBTDevices.isNotEmpty()) {
			connectBTDevice(knownBTDevices.first())
			return //having multiple watches is rare, user will have to select from DeviceSelect
		}
		//Check if Garmin connect app is installed
		try {
			activity.packageManager.getPackageInfo("com.garmin.android.apps.connectmobile", 0)
		} catch(_: PackageManager.NameNotFoundException) {
			iQSdkStatus = IQSdkStatus.GCM_NOT_INSTALLED
			status.value = Status.DISCONNECTED
			return
		}
		val knownIQIdsStrings = sharedPreferences?.getStringSet("knownIQIds", null) ?: mutableSetOf()
		knownIQIdsStrings.forEach(Consumer { knownIQIds.add(it.toLong()) })
		logD("Comms.start " + knownIQIds.size + " known IQ devices")

		if(connectIQ == null) {
			connectIQ = ConnectIQ.getInstance(
				activity,
				if(EMULATOR_MODE) ConnectIQ.IQConnectType.TETHERED else ConnectIQ.IQConnectType.WIRELESS
			)
			tryIgnore { Looper.prepare() } //connectIQ needs this
			connectIQ?.initialize(activity, false, this)
			//The rest of start will be done in onSdkReady
		} else {
			status.value = Status.DISCONNECTED
		}
	}

	fun checkKnownBTAddress(knownAddress: String, bondedDevices: Set<BluetoothDevice>) {
		for (device in bondedDevices) {
			if (device.address == knownAddress) {
				knownBTDevices.add(device)
				return
			}
		}
		delKnownBTAddress(knownAddress)
	}
	fun delKnownBTAddress(address: String) {
		if (knownBTAddresses.remove(address)) storeKnownBTAddresses()
	}
	fun storeKnownBTAddresses() {
		sharedPreferences?.edit {
			if (knownBTAddresses.isEmpty()) {
				remove("knownBTAddresses")
			} else {
				putStringSet("knownBTAddresses", knownBTAddresses)
			}
		}
	}

	//Check if a known device is still bound, if so, add it to known devices, else remove
	fun checkKnownIQDevice(id: Long, bondedDevices: List<IQDevice>) {
		for(device in bondedDevices) {
			if(device.deviceIdentifier == id) {
				knownIQDevices.add(device)
				return
			}
		}
		delKnownIQId(id)
	}
	fun delKnownIQId(id: Long) {
		if(!knownIQIds.contains(id)) return
		knownIQIds.remove(id)
		storeKnownIQIds()
	}
	fun addKnownIQDevice(device: IQDevice) {
		if(knownIQDevices.contains(device)) return
		knownIQDevices.add(device)
		addKnownIQDeviceId(device.deviceIdentifier)
	}
	fun addKnownIQDeviceId(id: Long) {
		if(knownIQIds.contains(id)) return
		knownIQIds.add(id)
		storeKnownIQIds()
	}
	fun storeKnownIQIds() {
		sharedPreferences?.edit {
			if (knownIQIds.isEmpty()) {
				remove("knownIQIds")
			} else {
				putStringSet("knownIQIds", knownIQIds.map { it.toString() }.toSet())
			}
		}
	}

	fun stop() {
		disconnect = true
		requestQueue.clear()
		lastRequest = null
		status.value = Status.DISCONNECTED
		messageStatus = -1
		tryIgnore { bluetoothSocket?.close() }
		bluetoothSocket = null
		commsBTConnect = null
		commsBTConnected = null
		iQDevice = null
		iQApp = null
	}
	fun onDestroy(context: Context) {
		if(connectIQ != null) {
			tryIgnore { connectIQ?.unregisterAllForEvents() }
			tryIgnore { connectIQ?.shutdown(context) }
			connectIQ = null
		}
	}

	fun syncIfConnected() {
		if(status.value in setOf(Status.CONNECTED_BT, Status.CONNECTED_IQ))
			sendRequest(Request(Request.Type.SYNC))
	}
	fun sendRequestPrep(settings: JSONObject) =
		sendRequest(Request(Request.Type.PREPARE, settings = settings))
	fun sendRequest(request: Request) {
		requestQueue.add(request)
		runInBackground(this::sendNextIQMessage)
	}

	suspend fun gotResponse(response: JSONObject) {
		try {
			val requestType = response.getString("requestType")
			when (requestType) {
				"sync" -> {
					//{requestType":"sync","responseData":{"match_ids":[],"settings":{ settings }}}
					//settings: {"home_name":"home","home_color":"white","away_name":"away","away_color":"orange","match_type":"15s","period_time":40,"period_count":2,"sinbin":10,"points_try":5,"points_con":2,"points_goal":3,"clock_pk":60,"clock_con":60,"clock_restart":0,"screen_on":true,"timer_type":1,"record_player":false,"record_pens":false,"delay_end":false,"help_version":6}
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
					//{"requestType":"getMatch","responseData":{ match }}
					val matchJson = response.getJSONObject("responseData")
					watchMatch.emit(MatchData(matchJson, true))
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
			logE("Comms.gotResponse: $e")
			logE("Comms.gotResponse: " + e.message)
			onMessageError(R.string.fail_response)
		}
		lastRequest = null
	}
	fun gotMatchIds(matchIds: Set<Long>) {
		matchIds.forEach { matchId ->
			if(MatchStore.deletedMatches.contains(matchId)) {
				sendRequest(Request(Request.Type.DEL_MATCH, matchId))
			} else if (MatchStore.matches.none { it.matchId == matchId }) {
				sendRequest(Request(Request.Type.GET_MATCH, matchId))
			}
		}
		MatchStore.deletedMatches.removeIf { !matchIds.contains(it) }
	}

	class Request(val type: Type, val matchId: Long? = null, val settings: JSONObject? = null) {
		enum class Type { SYNC, GET_MATCH, DEL_MATCH, PREPARE }
		override fun toString(): String {
			val request = JSONObject()
			request.put("version", 2)//Jun 2025, removed deleted_matches from SYNC
			when (type) {
				Type.SYNC -> {
					//{"version":2,"requestType":"sync","requestData":{"custom_match_types":[]}}
					request.put("requestType", "sync")
					val requestData = JSONObject()
					val customMatchTypes = JSONArray()
					MatchStore.customMatchTypes.forEach { customMatchTypes.put(it.toJson()) }
					requestData.put("custom_match_types", customMatchTypes)
					request.put("requestData", requestData)
					messageStatus = R.string.sync
				}
				Type.GET_MATCH -> {
					request.put("requestType", "getMatch")
					request.put("requestData", matchId)
					messageStatus = R.string.get_match
				}
				Type.DEL_MATCH -> {
					request.put("requestType", "delMatch")
					request.put("requestData", matchId)
					messageStatus = R.string.del_match
				}
				Type.PREPARE -> {
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
		requestQueue.clear()
		lastRequest = null
	}
	fun onMessageError(message: Int) {
		messageStatus = message
		runInBackground { messageError.emit(message) }
	}
	fun getBondedBTDevices(): Set<BluetoothDevice>? {
		if (bluetoothAdapter == null) {
			onError(R.string.fail_BT_denied)
			return null
		}
		if (bluetoothAdapter?.isEnabled != true) {
			onError(R.string.fail_BT_off)
			return null
		}
		return bluetoothAdapter?.bondedDevices
	}
	fun connectBTDevice(device: BluetoothDevice) {
		logD("Comms.connectBTDevice: " + device.name)
		if (bluetoothAdapter == null) {
			onError(R.string.fail_BT_denied)
			return
		}
		if (bluetoothAdapter?.isEnabled != true) {
			onError(R.string.fail_BT_off)
			return
		}
		if (status.value in listOf(Status.CONNECTING, Status.CONNECTED_BT, Status.CONNECTED_IQ)) return
		disconnect = false
		deviceName = device.name ?: "<no name>"
		status.value = Status.CONNECTING
		commsBTConnect = CommsBTConnect(device)
		commsBTConnect?.start()
	}
	fun onCommsBTDisconnect() {
		if(status.value == Status.CONNECTING) onError(R.string.fail_connect)
		else status.value = Status.DISCONNECTED
		requestQueue.clear()
		lastRequest = null
		deviceName = ""
		messageStatus = -1
		commsBTConnect = null
		commsBTConnected = null
		bluetoothSocket = null
	}

	class CommsBTConnect(device: BluetoothDevice) : Thread() {
		init {
			logD("CommsBTConnect " + device.name)
			try {
				bluetoothSocket = device.createRfcommSocketToServiceRecord(RRW_UUID)
			} catch (e: Exception) {
				logE("CommsBTConnect Exception: " + e.message)
				onCommsBTDisconnect()
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
				onCommsBTDisconnect()
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
				status.value = Status.CONNECTED_BT
				knownBTDevices.add(bluetoothSocket!!.remoteDevice)
				if (knownBTAddresses.add(bluetoothSocket!!.remoteDevice.address)) {
					storeKnownBTAddresses()
				}
			} catch (e: Exception) {
				logE("CommsBTConnected init Exception: " + e.message)
				onCommsBTDisconnect()
			}
		}
		override fun run() {
			sendRequest(Request(Request.Type.SYNC))
			runBlocking { process() }
		}
		suspend fun process() {
			while (!disconnect) {
				try { outputStream!!.write("".toByteArray()) }
				catch (_: Exception) {
					logD("Connection closed")
					break
				}
				sendNextRequest()
				read()
				delay(100)
			}
			logD("CommsBTConnected.process: close")
			tryIgnore { bluetoothSocket?.close() }
			onCommsBTDisconnect()
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
			} catch (e: Exception) {
				logE("CommsBTConnected.read Exception: " + e.message)
			}
			lastRequest = null
			onMessageError(R.string.fail_response)
		}
		fun isValidJSON(json: String): Boolean {
			if (!json.endsWith("}")) return false
			try { JSONObject(json)} catch (_: JSONException) { return false }
			return true
		}
	}

	fun onIQStartDone() {
		if(status.value in listOf(Status.CONNECTING, Status.CONNECTED_BT, Status.ERROR)) return
		status.value = Status.DISCONNECTED
	}
	fun getBondedIQDevices(): List<IQDevice>? {
		if(iQSdkStatus != IQSdkStatus.READY) return null
		try { return connectIQ?.knownDevices }
		catch(e: Exception) { logE("Comms.getBondedIQDevices exception: " + e.message) }
		return null
	}
	fun connectIQDevice(device: IQDevice) {
		logD("Comms.connectIQDevice: " + device.friendlyName + " status: " + device.status)
		if (bluetoothAdapter == null) {
			onError(R.string.fail_BT_denied)
			return
		}
		if (bluetoothAdapter?.isEnabled != true) {
			onError(R.string.fail_BT_off)
			return
		}
		if (status.value in listOf(Status.CONNECTING, Status.CONNECTED_BT, Status.CONNECTED_IQ)) return
		disconnect = false
		deviceName = device.friendlyName
		status.value = Status.CONNECTING
		iQDevice = device
		try { connectIQ!!.getApplicationInfo(IQ_APP_ID, iQDevice, this) }
		catch(e: Exception) {
			logE("Comms.connectIQDevice exception: " + e.message)
			onError(R.string.fail_connect)
		}
	}
	private fun sendNextIQMessage() {
		if(status.value != Status.CONNECTED_IQ || requestQueue.isEmpty() || lastRequest != null) return
		try {
			lastRequest = requestQueue.first()
			requestQueue.remove(lastRequest)
			logD("Comms.sendNextIQMessage: $lastRequest")
			connectIQ!!.sendMessage(iQDevice, iQApp, lastRequest.toString()) {
				d: IQDevice?, a: IQApp?, messageStatus: ConnectIQ.IQMessageStatus? ->
				logD("Comms.sendNextIQMessage.onMessageStatus status: " + messageStatus!!.name + " " + iQApp!!.applicationId)
				if(messageStatus != ConnectIQ.IQMessageStatus.SUCCESS)
					onMessageError(R.string.fail_send_message)
			}
		} catch(e: java.lang.Exception) {
			logE("Comms.sendNextIQMessage exception: $e")
			logE("Comms.sendNextIQMessage exception: " + e.message)
			onMessageError(R.string.fail_send_message)
		}
	}
	//ConnectIQListener
	override fun onSdkReady() {
		logD("Comms.onSdkReady")
		iQSdkStatus = IQSdkStatus.READY
		//Resume IQ part of start
		val bondedIQDevices = getBondedIQDevices()
		if(bondedIQDevices == null) {
			onIQStartDone()
			return
		}
		//Find and clean known IQ devices
		for(id in knownIQIds) checkKnownIQDevice(id, bondedIQDevices)
		//Try to connect to known IQ device
		for(device in knownIQDevices) {
			connectIQDevice(device)
			return //having multiple watches is rare, user will have to select from DeviceSelect
		}
		onIQStartDone()
	}
	override fun onInitializeError(errorStatus: ConnectIQ.IQSdkErrorStatus?) {
		logD("Comms.onInitializeError: $errorStatus")
		iQSdkStatus = when(errorStatus) {
			ConnectIQ.IQSdkErrorStatus.GCM_NOT_INSTALLED -> IQSdkStatus.GCM_NOT_INSTALLED
			ConnectIQ.IQSdkErrorStatus.GCM_UPGRADE_NEEDED -> IQSdkStatus.GCM_UPGRADE_NEEDED
			ConnectIQ.IQSdkErrorStatus.SERVICE_ERROR -> IQSdkStatus.ERROR
			else -> IQSdkStatus.ERROR
		}
		onIQStartDone()
	}
	override fun onSdkShutDown() {
		logD("Comms.onSdkShutDown")
		iQSdkStatus = IQSdkStatus.UNAVAILABLE
	}
	//IQApplicationInfoListener
	override fun onApplicationInfoReceived(app: IQApp?) {
		logD("Comms.onApplicationInfoReceived $app")
		if(status.value in listOf(Status.CONNECTED_BT, Status.CONNECTED_IQ)) return
		if(app?.status == IQApp.IQAppStatus.INSTALLED || EMULATOR_MODE) {
			status.value = Status.CONNECTED_IQ
			iQApp = if(EMULATOR_MODE) IQApp("") else app //in the emulator the applicationID is empty
			try {
				connectIQ!!.registerForDeviceEvents(iQDevice, this)
				connectIQ!!.registerForAppEvents(iQDevice, iQApp, this)
				deviceName = iQDevice!!.friendlyName
				addKnownIQDevice(iQDevice!!)
				sendRequest(Request(Request.Type.SYNC))
			} catch(e: Exception) {
				logE("Comms.onApplicationInfoReceived exception: $e")
				logE("Comms.onApplicationInfoReceived exception: " + e.message)
				onError(R.string.fail_connect)
			}
		} else if(status.value == Status.CONNECTING) onError(R.string.fail_app_not_installed)
	}
	override fun onApplicationNotInstalled(applicationId: String?) {
		logD("Comms.onApplicationNotInstalled $applicationId")
		onError(R.string.fail_app_not_installed)
		iQDevice = null
	}
	override fun onDeviceStatusChanged(device: IQDevice?, statusNew: IQDevice.IQDeviceStatus?) {
		logD("Comms.onApplicationNotInstalled device: $device statusNew: $statusNew")
		if(statusNew == IQDevice.IQDeviceStatus.CONNECTED) {
			if(status.value in listOf(Status.CONNECTED_BT, Status.CONNECTED_IQ)) return
			iQDevice = device
			try {
				logD("getApplicationInfo")
				connectIQ!!.getApplicationInfo(IQ_APP_ID, device, this)
				//onApplicationInfoReceived/onApplicationNotInstalled will be called to make sure RRW is installed on the watch
			} catch(e: Exception) {
				logE("Comms.onApplicationInfoReceived exception: $e")
				logE("Comms.onApplicationInfoReceived exception: " + e.message)
				onError(R.string.fail_unexpected)
			}
		} else if(status.value == Status.CONNECTED_IQ) status.value = Status.DISCONNECTED
	}
	//IQApplicationEventListener
	override fun onMessageReceived(
		d: IQDevice?, a: IQApp?, data: List<Any?>?, status: ConnectIQ.IQMessageStatus?
	) {
		logD("Comms.onMessageReceived messageStatus: $status messageData: $data")
		lastRequest = null
		try {
			if(status != ConnectIQ.IQMessageStatus.SUCCESS) throw Exception()
			data!!.forEach(Consumer { message: Any? ->
				val messageJson = JSONObject(message as String)
				runInBackground { gotResponse(messageJson) }
			})
		} catch(e: Exception) {
			logE("Comms.onMessageReceived exception: $e")
			logE("Comms.onMessageReceived exception: " + e.message)
			onMessageError(R.string.fail_response)
		}
		sendNextIQMessage()
	}
}
