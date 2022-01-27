package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class FileStore{
    //Append a new match
    public static void file_storeMatch(Context context, MatchData match){
        ArrayList<MatchData> matches = file_readMatches(context);
        matches.add(match);
        file_storeMatches(context, matches);
    }
    //Store all merchants
    public static JSONArray file_storeMatches(Context context, ArrayList<MatchData> matches){
        JSONArray jaMatches = new JSONArray();
        try{
            for(int i=0; i < matches.size(); i++){
                jaMatches.put(matches.get(i).toJson(context));
            }
            storeFile(context, R.string.matches_filename, jaMatches.toString());
        }catch(Exception e){
            Log.e("FileStore", "file_storeMatches: " + e.getMessage());
            Toast.makeText(context, "Failed to save matches", Toast.LENGTH_SHORT).show();
        }
        return jaMatches;
    }
    //Return an array of stored matches
    public static ArrayList<MatchData> file_readMatches(Context context){
        ArrayList<MatchData> matches = new ArrayList<>();
        try{
            String sMatches = getFileAsString(context, R.string.matches_filename);
            if(sMatches.length() < 3){ return matches;}
            JSONArray jsonMatches = new JSONArray(sMatches);
            for(int i = 0; i < jsonMatches.length(); i++){
                matches.add(new MatchData(context, jsonMatches.getJSONObject(i)));
            }
        }catch(Exception e){
            Log.e("FileStore", "file_readMatches: " + e.getMessage());
            Toast.makeText(context, "Failed to read matches", Toast.LENGTH_SHORT).show();
        }
        return matches;
    }

    //Go through stored matches and remove old ones
    public static void file_cleanMatches(Context context){
        ArrayList<MatchData> matches = file_readMatches(context);
        for(int i = matches.size(); i > 0; i--){
            if(matches.get(i-1).match_id < MainActivity.getCurrentTimestamp() - (1000*60*60*24*14)){
                matches.remove(i-1);
            }
        }
        file_storeMatches(context, matches);
    }
    //The phone sends a list of matches that can be deleted
    public static JSONArray file_deletedMatches(Context context, String request_data){
        try{
            JSONObject request_data_jo = new JSONObject(request_data);
            JSONArray deleted_matches = request_data_jo.getJSONArray("deleted_matches");

            ArrayList<MatchData> matches = file_readMatches(context);
            for(int i = matches.size(); i > 0; i--){
                for(int j = 0; j < deleted_matches.length(); j++){
                    if(matches.get(i-1).match_id == deleted_matches.getLong(j)){
                        matches.remove(i-1);
                    }
                }
            }
            return file_storeMatches(context, matches);
        }catch(Exception e){
            Log.e("FileStore", "file_deletedMatches: " + e.getMessage());
            Toast.makeText(context, "Failed to delete matches", Toast.LENGTH_SHORT).show();
        }
        return new JSONArray();
    }
    public static void file_storeSettings(Context context){
        try{
            JSONObject jsonSettings = new JSONObject();
            jsonSettings.put("record_player", MainActivity.record_player);
            jsonSettings.put("screen_on", MainActivity.screen_on);
            jsonSettings.put("timer_type", MainActivity.timer_type);
            jsonSettings.put("match_type", MainActivity.match.match_type);
            jsonSettings.put("period_time", MainActivity.match.period_time);
            jsonSettings.put("period_count", MainActivity.match.period_count);
            jsonSettings.put("sinbin", MainActivity.match.sinbin);
            jsonSettings.put("points_try", MainActivity.match.points_try);
            jsonSettings.put("points_con", MainActivity.match.points_con);
            jsonSettings.put("points_goal", MainActivity.match.points_goal);
            storeFile(context, R.string.settings_filename, jsonSettings.toString());
        }catch(Exception e){
            Log.e("FileStore", "file_storeSettings: " + e.getMessage());
            Toast.makeText(context, "Failed to store settings", Toast.LENGTH_SHORT).show();
        }
    }
    public static void file_readSettings(Context context){
        try{
            String sSettings = getFileAsString(context, R.string.settings_filename);
            if(sSettings.length() < 3){ return;}
            JSONObject jsonSettings = new JSONObject(sSettings);
            MainActivity.record_player = jsonSettings.getBoolean("record_player");
            MainActivity.screen_on = jsonSettings.getBoolean("screen_on");
            MainActivity.timer_type = jsonSettings.getInt("timer_type");
            MainActivity.match.match_type = jsonSettings.getString("match_type");
            MainActivity.match.period_time = jsonSettings.getInt("period_time");
            MainActivity.match.period_count = jsonSettings.getInt("period_count");
            MainActivity.match.sinbin = jsonSettings.getInt("sinbin");
            MainActivity.match.points_try = jsonSettings.getInt("points_try");
            MainActivity.match.points_con = jsonSettings.getInt("points_con");
            MainActivity.match.points_goal = jsonSettings.getInt("points_goal");
        }catch(Exception e){
            Log.e("FileStore", "file_readSettings: " + e.getMessage());
            Toast.makeText(context, "Failed to read settings", Toast.LENGTH_SHORT).show();
        }
    }

    private static String getFileAsString(Context context, int file){
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
            Log.i("FileStore", "file does not exists yet");
        }catch(Exception e){
            Log.e("FileStore", "getFileAsString: " + e.getMessage());
            Toast.makeText(context, "Failed to read file", Toast.LENGTH_SHORT).show();
        }
        return "";
    }
    private static void storeFile(Context context, int file, String content){
        try{
            FileOutputStream fos = context.openFileOutput(context.getString(file), Context.MODE_PRIVATE);
            OutputStreamWriter osr = new OutputStreamWriter(fos);
            osr.write(content);
            osr.close();
        }catch(Exception e){
            Log.e("FileStore", "storeFile: " + e.getMessage());
            Toast.makeText(context, "Failed to store file", Toast.LENGTH_SHORT).show();
        }
    }

}
