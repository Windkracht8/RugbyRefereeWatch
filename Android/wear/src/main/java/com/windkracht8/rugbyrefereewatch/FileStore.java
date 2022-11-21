package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

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
            Log.e(MainActivity.RRW_LOG_TAG, "FileStore.file_storeMatches Exception: " + e.getMessage());
            MainActivity.makeToast(context, context.getString(R.string.fail_save_matches));
        }
    }
    //Return an array of stored matches
    public static JSONArray file_readMatches(Context context){
        try{
            String sMatches = getFileAsString(context, R.string.matches_filename);
            if(sMatches.length() < 3){return new JSONArray();}
            return new JSONArray(sMatches);
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "FileStore.file_readMatches Exception: " + e.getMessage());
            MainActivity.makeToast(context, context.getString(R.string.fail_read_matches));
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
            Log.e(MainActivity.RRW_LOG_TAG, "FileStore.file_cleanMatches Exception: " + e.getMessage());
            MainActivity.makeToast(context, context.getString(R.string.fail_clean_matches));
        }
    }
    //The phone sends a list of matches that can be deleted
    public static JSONArray file_deletedMatches(Context context, String request_data){
        try{
            JSONObject request_data_jo = new JSONObject(request_data);
            JSONArray deleted_matches = request_data_jo.getJSONArray("deleted_matches");

            JSONArray matches = file_readMatches(context);
            for(int d = 0; d < deleted_matches.length(); d++){
                for(int m = matches.length()-1; m >= 0; m--){
                    JSONObject match = matches.getJSONObject(m);
                    if(match.getLong("matchid") == deleted_matches.getLong(d)){
                        matches.remove(m);
                    }
                }
            }
            file_storeMatches(context, matches);
            return matches;
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "FileStore.file_deletedMatches Exception: " + e.getMessage());
            MainActivity.makeToast(context, context.getString(R.string.fail_del_matches));
        }
        return new JSONArray();
    }
    public static void file_storeSettings(Context context){
        try{
            JSONObject jsonSettings = new JSONObject();
            jsonSettings.put("record_player", MainActivity.record_player);
            jsonSettings.put("record_pens", MainActivity.record_pens);
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
            jsonSettings.put("home_color", MainActivity.match.home.color);
            jsonSettings.put("away_color", MainActivity.match.away.color);
            storeFile(context, R.string.settings_filename, jsonSettings.toString());
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "FileStore.file_storeSettings Exception: " + e.getMessage());
            MainActivity.makeToast(context, context.getString(R.string.fail_store_settings));
        }
    }
    public static void file_readSettings(Context context){
        int help_version = 0;
        try{
            String sSettings = getFileAsString(context, R.string.settings_filename);
            if(sSettings.length() < 3){return;}
            JSONObject jsonSettings = new JSONObject(sSettings);
            MainActivity.readSettings(context, jsonSettings);
            if(jsonSettings.has("help_version"))
                help_version = jsonSettings.getInt("help_version");
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "FileStore.file_readSettings Exception: " + e.getMessage());
            MainActivity.makeToast(context, context.getString(R.string.fail_read_settings));
        }
        Intent intent = new Intent("com.windkracht8.rugbyrefereewatch");
        intent.putExtra("intent_type", "showHelp");
        intent.putExtra("help_version", help_version);
        context.sendBroadcast(intent);
    }

    public static void file_storeCustomMatchTypes(Context context){
        try{
            FileOutputStream fos = context.openFileOutput(context.getString(R.string.match_types_filename), Context.MODE_PRIVATE);
            OutputStreamWriter osr = new OutputStreamWriter(fos);
            osr.write(Conf.customMatchTypes.toString());
            osr.close();
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "FileStore.file_storeCustomMatchTypes Exception: " + e.getMessage());
            MainActivity.makeToast(context, context.getString(R.string.fail_store_match_types));
        }
    }
    public static void file_readCustomMatchTypes(Context context){
        try{
            FileInputStream fis = context.openFileInput(context.getString(R.string.match_types_filename));
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder text = new StringBuilder();
            String line;
            while((line = br.readLine()) != null){
                text.append(line);
            }
            br.close();
            String sMatches = text.toString();
            JSONArray jsonMatchTypes = new JSONArray(sMatches);
            for(int i = 0; i < jsonMatchTypes.length(); i++){
                Conf.customMatchTypes.put(jsonMatchTypes.getJSONObject(i));
            }
        }catch(FileNotFoundException e){
            Log.i(MainActivity.RRW_LOG_TAG, "FileStore.file_readCustomMatchTypes Match types file does not exists yet");
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "FileStore.file_readCustomMatchTypes Exception: " + e.getMessage());
            MainActivity.makeToast(context, context.getString(R.string.fail_read_match_types));
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
            Log.i(MainActivity.RRW_LOG_TAG, "FileStore.getFileAsString File does not exist yet");
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "FileStore.getFileAsString Exception: " + e.getMessage());
            MainActivity.makeToast(context, context.getString(R.string.fail_read_file));
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
            Log.e(MainActivity.RRW_LOG_TAG, "FileStore.storeFile Exception: " + e.getMessage());
            MainActivity.makeToast(context, context.getString(R.string.fail_store_file));
        }
    }

}
