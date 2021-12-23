package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class Filestore {
    //Append a new match
    public static void file_storeMatch(Context context, matchdata match){
        ArrayList<matchdata> matches = file_readMatches(context);
        matches.add(match);
        file_storeMatches(context, matches);
    }
    //Store all merchants
    public static String file_storeMatches(Context context, ArrayList<matchdata> matches){
        try {
            JSONArray jsonaMatches = new JSONArray();
            for (int i=0; i < matches.size(); i++) {
                jsonaMatches.put(matches.get(i).tojson());
            }
            storeFile(context, R.string.matches_filename, jsonaMatches.toString());
            return jsonaMatches.toString();
        } catch (Exception e) {
            Log.e("Filestore", "file_storeMatches: " + e.getMessage());
        }
        return "";
    }
    //Return an array of stored matches
    public static ArrayList<matchdata> file_readMatches(Context context){
        ArrayList<matchdata> matches = new ArrayList<>();
        try {
            String sMatches = getFileAsString(context, R.string.matches_filename);
            Log.i("Filestore", "file_readMatches: " + sMatches);
            if(sMatches.length() < 3){ return matches;}
            JSONArray jsonMatches = new JSONArray(sMatches);
            for (int i = 0; i < jsonMatches.length(); i++) {
                matches.add(new matchdata(jsonMatches.getJSONObject(i)));
            }
        } catch (Exception e) {
            Log.e("Filestore", "file_readMatches: " + e.getMessage());
        }
        return matches;
    }

    //Go through stored matches and remove old ones
    public static void file_cleanMatches(Context context){
        ArrayList<matchdata> matches = file_readMatches(context);
        for (int i = matches.size(); i > 0; i--) {
            if(matches.get(i-1).matchid < MainActivity.getCurrentTimestamp() - (1000*60*60*24*14)){
                matches.remove(i-1);
            }
        }
        file_storeMatches(context, matches);
    }
    //The phone sends a list of matches that can be deleted
    public static String file_deletedMatches(Context context, String requestdata){
        try{
            JSONObject requestdata_json = new JSONObject(requestdata);
            JSONArray deleted_matches = requestdata_json.getJSONArray("deleted_matches");

            ArrayList<matchdata> matches = file_readMatches(context);
            for (int i = matches.size(); i > 0; i--) {
                for (int j = 0; j < deleted_matches.length(); j++) {
                    if(matches.get(i-1).matchid == deleted_matches.getLong(j)){
                        matches.remove(i-1);
                    }
                }
            }
            return file_storeMatches(context, matches);
        } catch (Exception e) {
            Log.e("Filestore", "file_deletedMatches: " + e.getMessage());
        }
        return "";
    }
    public static void file_storeSettings(Context context){
        try{
            JSONObject jsonSettings = new JSONObject();
            jsonSettings.put("screen_on", MainActivity.screen_on);
            jsonSettings.put("countdown", MainActivity.countdown);
            jsonSettings.put("match_type", MainActivity.match.match_type);
            jsonSettings.put("period_time", MainActivity.match.period_time);
            jsonSettings.put("period_count", MainActivity.match.period_count);
            jsonSettings.put("sinbin", MainActivity.match.sinbin);
            jsonSettings.put("points_try", MainActivity.match.points_try);
            jsonSettings.put("points_con", MainActivity.match.points_con);
            jsonSettings.put("points_goal", MainActivity.match.points_goal);
            storeFile(context, R.string.settings_filename, jsonSettings.toString());
        } catch (Exception e) {
            Log.e("Filestore", "file_storeSettings: " + e.getMessage());
        }
    }
    public static void file_readSettings(Context context){
        try {
            String sSettings = getFileAsString(context, R.string.settings_filename);
            if(sSettings.length() < 3){ return;}
            JSONObject jsonSettings = new JSONObject(sSettings);
            MainActivity.screen_on = jsonSettings.getBoolean("screen_on");
            MainActivity.countdown = jsonSettings.getBoolean("countdown");
            MainActivity.match.match_type = jsonSettings.getString("match_type");
            MainActivity.match.period_time = jsonSettings.getInt("period_time");
            MainActivity.match.period_count = jsonSettings.getInt("period_count");
            MainActivity.match.sinbin = jsonSettings.getInt("sinbin");
            MainActivity.match.points_try = jsonSettings.getInt("points_try");
            MainActivity.match.points_con = jsonSettings.getInt("points_con");
            MainActivity.match.points_goal = jsonSettings.getInt("points_goal");
        } catch (Exception e) {
            Log.e("Filestore", "file_readSettings: " + e.getMessage());
        }
    }

    private static String getFileAsString(Context context, int file){
        try {
            FileInputStream fis = context.openFileInput(context.getString(file));
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder text = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {text.append(line);}
            br.close();
            return text.toString();
        } catch (Exception e) {
            Log.e("Filestore", "getFileAsString: " + e.getMessage());
        }
        return "";
    }
    private static void storeFile(Context context, int file, String content){
        try {
            FileOutputStream fos = context.openFileOutput(context.getString(file), Context.MODE_PRIVATE);
            OutputStreamWriter osr = new OutputStreamWriter(fos);
            osr.write(content);
            osr.close();
        } catch (Exception e) {
            Log.e("Filestore", "storeFile: " + e.getMessage());
        }
    }

}
