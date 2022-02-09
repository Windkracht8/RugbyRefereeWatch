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

public class FileStore{
    //Append a new match
    public static void file_storeMatch(Context context, MatchData match){
        JSONArray matches = file_readMatches(context);
        matches.put(match.toJson(context));
        file_storeMatches(context, matches);
    }

    //Store all matches
    public static void file_storeMatches(Context context, JSONArray matches){
        try{
            storeFile(context, R.string.matches_filename, matches.toString());
        }catch(Exception e){
            Log.e("FileStore", "file_storeMatches: " + e.getMessage());
            Toast.makeText(context, "Failed to save matches", Toast.LENGTH_SHORT).show();
        }
    }
    //Return an array of stored matches
    public static JSONArray file_readMatches(Context context){
        try{
            String sMatches = getFileAsString(context, R.string.matches_filename);
            if(sMatches.length() < 3){return new JSONArray();}
            return new JSONArray(sMatches);
        }catch(Exception e){
            Log.e("FileStore", "file_readMatches: " + e.getMessage());
            Toast.makeText(context, "Failed to read matches", Toast.LENGTH_SHORT).show();
        }
        return new JSONArray();
    }

    //Go through stored matches and remove old ones
    public static void file_cleanMatches(Context context){
        JSONArray matches = file_readMatches(context);
        try{
            for(int i = matches.length(); i > 0; i--){
                JSONObject match = matches.getJSONObject(i-1);
                if(match.getLong("matchid") < MainActivity.getCurrentTimestamp() - 1209600000){
                    matches.remove(i-1);
                }
            }
            file_storeMatches(context, matches);
        }catch(Exception e){
            Log.e("FileStore", "file_cleanMatches: " + e.getMessage());
            Toast.makeText(context, "Failed to cleanup matches", Toast.LENGTH_SHORT).show();
        }
    }
    //The phone sends a list of matches that can be deleted
    public static JSONArray file_deletedMatches(Context context, String request_data){
        try{
            JSONObject request_data_jo = new JSONObject(request_data);
            JSONArray deleted_matches = request_data_jo.getJSONArray("deleted_matches");

            JSONArray matches = file_readMatches(context);
            for(int i = matches.length(); i > 0; i--){
                for(int j = 0; j < deleted_matches.length(); j++){
                    JSONObject match = matches.getJSONObject(i-1);
                    if(match.getLong("matchid") == deleted_matches.getLong(j)){
                        matches.remove(i-1);
                    }
                }
            }
            file_storeMatches(context, matches);
            return matches;
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
            jsonSettings.put("help_version", MainActivity.help_version);
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
    public static JSONObject file_readSettings(Context context){
        try{
            String sSettings = getFileAsString(context, R.string.settings_filename);
            if(sSettings.length() < 3){return null;}
            return new JSONObject(sSettings);
        }catch(Exception e){
            Log.e("FileStore", "file_readSettings: " + e.getMessage());
            Toast.makeText(context, "Failed to read settings", Toast.LENGTH_SHORT).show();
        }
        return null;
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
