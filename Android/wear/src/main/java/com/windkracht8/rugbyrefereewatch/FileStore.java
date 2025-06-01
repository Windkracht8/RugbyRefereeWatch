/*
 * Copyright 2020-2025 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.windkracht8.rugbyrefereewatch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

//Thread: All of FileStore runs on a background thread
class FileStore{
    static JSONArray customMatchTypes = new JSONArray();

    static void storeMatch(Activity activity){
        try{
            JSONArray matches = readMatches(activity);
            matches.put(Main.match.toJson(activity));
            storeFile(activity, R.string.matches_filename, matches.toString());
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "FileStore.storeMatches Exception: " + e.getMessage());
            toast(activity, R.string.fail_store_match);
        }
    }

    static JSONArray readMatches(Context context) throws Exception{
        String sMatches = getFileAsString(context, R.string.matches_filename);
        if(sMatches.length() < 3){return new JSONArray();}
        return new JSONArray(sMatches);
    }
    static JSONArray readMatchIds(Context context) throws Exception{
        JSONArray matchIds = new JSONArray();
        JSONArray matches = readMatches(context);
        for(int i=0; i<matches.length(); i++)
            matchIds.put(matches.getJSONObject(i).getLong("matchid"));
        return matchIds;
    }
    static JSONObject readMatch(Context context, long match_id) throws Exception{
        JSONArray matches = readMatches(context);
        for(int i=0; i<matches.length(); i++){
            if(matches.getJSONObject(i).getLong("matchid") == match_id){
                return matches.getJSONObject(i);
            }
        }
        throw new Exception("Match not found");
    }
    static void delMatch(Context context, long match_id) throws Exception{
        JSONArray matches = readMatches(context);
        for(int i=0; i<matches.length(); i++){
            if(matches.getJSONObject(i).getLong("matchid") == match_id){
                matches.remove(i);
                storeFile(context, R.string.matches_filename, matches.toString());
                return;
            }
        }
        throw new Exception("Match not found");
    }
    //Go through stored matches and remove old ones
    static void cleanMatches(Activity activity){
        try{
            JSONArray matches = readMatches(activity);
            for(int i=matches.length()-1; i>=0; i--){
                if(matches.getJSONObject(i).getLong("matchid") < System.currentTimeMillis() - 2592000000L){
                    matches.remove(i);
                }
            }
            storeFile(activity, R.string.matches_filename, matches.toString());
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "FileStore.cleanMatches Exception: " + e.getMessage());
            toast(activity, R.string.fail_clean_matches);
        }
    }
    //The phone sends a list of matches that can be deleted
    static JSONArray deletedMatches(Context context, JSONArray deleted_matches) throws Exception{
        JSONArray matches = readMatches(context);
        for(int d=0; d<deleted_matches.length(); d++){
            for(int m=matches.length()-1; m>=0; m--){
                if(matches.getJSONObject(m).getLong("matchid") == deleted_matches.getLong(d)){
                    matches.remove(m);
                    break;
                }
            }
        }
        storeFile(context, R.string.matches_filename, matches.toString());
        return matches;
    }
    static void storeSettings(Activity activity){
        try{
            storeFile(activity, R.string.settings_filename, Main.getSettings().toString());
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "FileStore.storeSettings Exception: " + e.getMessage());
            toast(activity, R.string.fail_store_settings);
        }
    }
    static void readSettings(Main main){
        try{
            String sSettings = getFileAsString(main, R.string.settings_filename);
            if(sSettings.length() < 3){
                main.startActivity((new Intent(main, Help.class)).putExtra("showWelcome", true));
                storeSettings(main);
                return;
            }
            JSONObject jsonSettings = new JSONObject(sSettings);
            main.readSettings(jsonSettings);
        }catch(Exception e){
            main.startActivity((new Intent(main, Help.class)).putExtra("showWelcome", true));
            storeSettings(main);
            Log.e(Main.LOG_TAG, "FileStore.readSettings Exception: " + e.getMessage());
        }
    }

    private static void storeCustomMatchTypes(Context context) throws IOException{
        FileOutputStream fos = context.openFileOutput(context.getString(R.string.match_types_filename), Context.MODE_PRIVATE);
        OutputStreamWriter osr = new OutputStreamWriter(fos);
        osr.write(customMatchTypes.toString());
        osr.close();
    }
    static void readCustomMatchTypes(Activity activity){
        try{
            FileInputStream fis = activity.openFileInput(activity.getString(R.string.match_types_filename));
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder text = new StringBuilder();
            String line;
            while((line = br.readLine()) != null) text.append(line);
            br.close();
            customMatchTypes = new JSONArray(text.toString());
        }catch(FileNotFoundException e){
            Log.d(Main.LOG_TAG, "FileStore.readCustomMatchTypes Match types file does not exists yet");
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "FileStore.readCustomMatchTypes Exception: " + e.getMessage());
            toast(activity, R.string.fail_read_match_types);
        }
    }
    static void syncCustomMatchTypes(Context context, JSONArray custom_match_types) throws Exception{
        customMatchTypes = custom_match_types;
        storeCustomMatchTypes(context);
    }
    private static String getFileAsString(Context context, int file) throws IOException{
        try{
            FileInputStream fis = context.openFileInput(context.getString(file));
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder text = new StringBuilder();
            String line;
            while((line = br.readLine()) != null){text.append(line);}
            br.close();
            return text.toString();
        }catch(FileNotFoundException e){
            Log.i(Main.LOG_TAG, "FileStore.getFileAsString File does not exist yet");
        }
        return "";
    }
    private static void storeFile(Context context, int file, String content) throws IOException{
        FileOutputStream fos = context.openFileOutput(context.getString(file), Context.MODE_PRIVATE);
        OutputStreamWriter osr = new OutputStreamWriter(fos);
        osr.write(content);
        osr.close();
    }
    private static void toast(Activity activity, int message){
        activity.runOnUiThread(()->Toast.makeText(activity, message, Toast.LENGTH_SHORT).show());
    }
}
