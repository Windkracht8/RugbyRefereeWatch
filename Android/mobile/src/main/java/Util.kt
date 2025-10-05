/*
 * Copyright 2020-2025 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.windkracht8.rugbyrefereewatch

import android.content.Context
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

const val LOG_TAG = "RugbyRefereeWatch"
fun logE(message: String) = Log.e(LOG_TAG, message)
fun logD(message: () -> String) { if(BuildConfig.DEBUG) { Log.d(LOG_TAG, message()) } }

fun Context.toast(message: Int) =
	Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

fun JSONObject.getIntOrNull(key: String): Int? =
	try { getInt(key) } catch (_: Exception) { null }
fun JSONObject.getStringOrNull(key: String): String? =
	try { getString(key) } catch (_: Exception) { null }
fun JSONObject.getJSONArrayOrEmpty(key: String): JSONArray =
	try { getJSONArray(key) } catch (_: Exception) { JSONArray() }
fun JSONObject.getJSONObjectOrEmpty(key: String): JSONObject =
	try { getJSONObject(key) } catch (_: Exception) { JSONObject() }
fun JSONArray.getJSONObjectOrEmpty(key: Int): JSONObject =
	try { getJSONObject(key) } catch (_: Exception) { JSONObject() }

fun tryIgnore(block: () -> Unit) = try { block() } catch (_: Exception) {}
fun runInBackground(block: suspend () -> Unit) = CoroutineScope(Dispatchers.Default).launch { block() }
