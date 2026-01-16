/*
 * Copyright 2020-2026 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.windkracht8.rugbyrefereewatch

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.BufferedWriter
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.OutputStreamWriter

object MatchStore {
	const val MF: String = "matches.json"
	const val DMF: String = "deleted_matches.json"
	const val MTF: String = "match_types.json"
	val matches: SnapshotStateList<MatchData> = mutableStateListOf()
	val deletedMatches = emptySet<Long>().toMutableSet()
	val customMatchTypes: SnapshotStateList<MatchType> = mutableStateListOf()
	val customMatchTypeNames: SnapshotStateList<String> = mutableStateListOf()

	suspend fun read(activity: Activity) {
		try {
			val text = activity.openFileInput(MF).bufferedReader().use { it.readText() }
			val matchesJson = JSONArray(text)
			CoroutineScope(Dispatchers.Main).launch {
				for (i in 0..<matchesJson.length()) {
					matchesJson.optJSONObject(i)?.let { matchJson ->
						val match = MatchData(matchJson)
						if(matches.none { it.matchId == match.matchId }) matches.add(match)
					}
				}
				matches.sort()
			}
		} catch (_: FileNotFoundException) {
			storeMatches(activity)
		} catch (e: Exception) {
			logE("MatchStore.read matches Exception: ${e.message}")
			error.emit(R.string.fail_read_matches)
			return
		}
		try {
			val text = activity.openFileInput(DMF).bufferedReader().use { it.readText() }
			val deletedMatchesJson = JSONArray(text)
			for (i in 0..<deletedMatchesJson.length())
				deletedMatches.add(deletedMatchesJson.getLong(i))
		} catch (_: FileNotFoundException) {
			storeDeletedMatches(activity)
		} catch (e: Exception) {
			logE("MatchStore.read deletedMatches Exception: ${e.message}")
		}
		try {
			val text = activity.openFileInput(MTF).bufferedReader().use { it.readText() }
			val jsonArray = JSONArray(text)
			CoroutineScope(Dispatchers.Main).launch {
				for(i in 0..<jsonArray.length()) {
					jsonArray.optJSONObject(i)?.let { matchTypeJson ->
						customMatchTypes.add(MatchType(matchTypeJson))
					}
				}
				customMatchTypes.forEach { customMatchTypeNames.add(it.name) }
				customMatchTypeNames.sort()
			}
		} catch (_: FileNotFoundException) {
			storeCustomMatchTypes(activity)
		} catch (e: Exception) {
			logE("MatchStore.read customMatchTypes Exception: ${e.message}")
			error.emit(R.string.fail_read_custom_match_types)
		}
	}
	suspend fun storeMatches(activity: Activity) {
		logD{"MatchStore.storeMatches"}
		try {
			val matchesJson = JSONArray()
			matches.forEach { matchesJson.put(it.toJson()) }
			store(activity, MF, matchesJson)
		} catch (e: Exception) {
			logE("MatchStore.storeMatches Exception: ${e.message}")
			error.emit(R.string.fail_save_matches)
		}
	}
	suspend fun storeDeletedMatches(activity: Activity) {
		logD{"MatchStore.storeDeletedMatches"}
		try {
			val deletedMatchesJson = JSONArray()
			deletedMatches.forEach { deletedMatchesJson.put(it) }
			store(activity, DMF, deletedMatchesJson)
		} catch (e: Exception) {
			logE("MatchStore.storeDeletedMatches Exception: ${e.message}")
			error.emit(R.string.fail_save_matches)
		}
	}
	suspend fun storeCustomMatchTypes(activity: Activity) {
		logD{"MatchStore.storeCustomMatchTypes"}
		try {
			val customMatchTypesJson = JSONArray()
			customMatchTypes.forEach { customMatchTypesJson.put(it.toJson()) }
			store(activity, MTF, customMatchTypesJson)
		} catch (e: Exception) {
			logE("MatchStore.storeCustomMatchTypes Exception: ${e.message}")
			error.emit(R.string.fail_save_match_type)
		}
	}
	fun store(activity: Activity, filename: String, content: JSONArray) {
		val fos: FileOutputStream = activity.openFileOutput(filename, Context.MODE_PRIVATE)
		val osr = OutputStreamWriter(fos)
		osr.write(content.toString())
		osr.close()
	}
	suspend fun importMatches(activity: Activity, uri: Uri) {
		try {
			activity.contentResolver.openInputStream(uri)?.use { inputStream ->
				val text = inputStream.bufferedReader().use { it.readText() }
				val matchesJson = JSONArray(text)
				CoroutineScope(Dispatchers.Main).launch {
					for (i in 0..<matchesJson.length()) {
						//logD{"match: " + matchesJson.getJSONObject(i).toString())
						val match = MatchData(
							matchJson = matchesJson.getJSONObject(i),
							checkTeamNames = true
						)
						if (matches.none { it.matchId == match.matchId }) matches.add(match)
					}
					matches.sort()
					runInBackground { storeMatches(activity) }
				}
			}
		} catch (e: Exception) {
			logE("MatchStore.importMatches Exception: ${e.message}")
			error.emit(R.string.fail_import)
		}
	}
	fun deleteMatches(activity: Activity, matchIds: Set<Long>) {
		logD{"MatchStore.deleteMatches"}
		matchIds.forEach { matchId ->
			matches.removeIf { it.matchId == matchId }//this needs to happen on the UI thread
			deletedMatches.add(matchId)
		}
		runInBackground {
			storeMatches(activity)
			storeDeletedMatches(activity)
		}
	}
	suspend fun exportMatches(activity: Activity, uri: Uri, matchIds: Set<Long>) {
		logD{"MatchStore.exportMatches"}
		try {
			val matchesJson = JSONArray()
			matches.forEach {
				if (matchIds.isEmpty() || matchIds.contains(it.matchId))
					matchesJson.put(it.toJson())
			}
			activity.contentResolver.openOutputStream(uri)?.use { os ->
				BufferedWriter(OutputStreamWriter(os)).use { writer ->
					writer.write(matchesJson.toString())
				}
			}
		} catch (e: Exception) {
			logE("MatchStore.exportMatches Exception: ${e.message}")
			error.emit(R.string.fail_export)
		}
	}
	fun saveMatch(activity: Activity, matchData: MatchData) {
		logD{"MatchStore.saveMatch"}
		val index = matches.indexOfFirst { it.matchId == matchData.matchId }
		if (index != -1) matches[index] = matchData//this needs to happen on the UI thread
		else matches.add(matchData)//this needs to happen on the UI thread
		runInBackground { storeMatches(activity) }
	}
}
