package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.os.Handler;
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
    public static void storeMatch(Context context, Handler hMessage, MatchData match){
        JSONArray matches = readMatches(context, hMessage);
        matches.put(match.toJson(context));
        storeMatches(context, hMessage, matches);
    }

    //Store all matches
    public static void storeMatches(Context context, Handler hMessage, JSONArray matches){
        try{
            storeFile(context, hMessage, R.string.matches_filename, matches.toString());
        }catch(Exception e){
            Log.e(Main.RRW_LOG_TAG, "FileStore.file_storeMatches Exception: " + e.getMessage());
            hMessage.sendMessage(hMessage.obtainMessage(Main.MESSAGE_TOAST, R.string.fail_save_matches));
        }
    }
    //Return an array of stored matches
    public static JSONArray readMatches(Context context, Handler hMessage){
        try{
            String sMatches = getFileAsString(context, hMessage, R.string.matches_filename);
            if(sMatches.length() < 3){return new JSONArray();}
            return new JSONArray(sMatches);
        }catch(Exception e){
            Log.e(Main.RRW_LOG_TAG, "FileStore.file_readMatches Exception: " + e.getMessage());
            hMessage.sendMessage(hMessage.obtainMessage(Main.MESSAGE_TOAST, R.string.fail_read_matches));
        }
        return new JSONArray();
    }

    //Go through stored matches and remove old ones
    public static void cleanMatches(Context context, Handler hMessage){
        JSONArray matches = readMatches(context, hMessage);
        try{
            for(int i = matches.length(); i > 0; i--){
                JSONObject match = matches.getJSONObject(i-1);
                if(match.getLong("matchid") < Main.getCurrentTimestamp() - 1209600000){
                    matches.remove(i-1);
                }
            }
            storeMatches(context, hMessage, matches);
        }catch(Exception e){
            Log.e(Main.RRW_LOG_TAG, "FileStore.file_cleanMatches Exception: " + e.getMessage());
            hMessage.sendMessage(hMessage.obtainMessage(Main.MESSAGE_TOAST, R.string.fail_clean_matches));
        }
    }
    //The phone sends a list of matches that can be deleted
    public static JSONArray deletedMatches(Context context, Handler hMessage, String request_data){
        try{
            JSONObject request_data_jo = new JSONObject(request_data);
            JSONArray deleted_matches = request_data_jo.getJSONArray("deleted_matches");

            JSONArray matches = readMatches(context, hMessage);
            for(int d = 0; d < deleted_matches.length(); d++){
                for(int m = matches.length()-1; m >= 0; m--){
                    JSONObject match = matches.getJSONObject(m);
                    if(match.getLong("matchid") == deleted_matches.getLong(d)){
                        matches.remove(m);
                    }
                }
            }
            storeMatches(context, hMessage, matches);
            return matches;
        }catch(Exception e){
            Log.e(Main.RRW_LOG_TAG, "FileStore.file_deletedMatches Exception: " + e.getMessage());
            hMessage.sendMessage(hMessage.obtainMessage(Main.MESSAGE_TOAST, R.string.fail_del_matches));
        }
        return new JSONArray();
    }
    public static void storeSettings(Context context, Handler hMessage){
        try{
            JSONObject jsonSettings = new JSONObject();
            jsonSettings.put("record_player", Main.record_player);
            jsonSettings.put("record_pens", Main.record_pens);
            jsonSettings.put("bluetooth", Main.bluetooth);
            jsonSettings.put("screen_on", Main.screen_on);
            jsonSettings.put("timer_type", Main.timer_type);
            jsonSettings.put("help_version", Main.help_version);
            jsonSettings.put("match_type", Main.match.match_type);
            jsonSettings.put("period_time", Main.match.period_time);
            jsonSettings.put("period_count", Main.match.period_count);
            jsonSettings.put("sinbin", Main.match.sinbin);
            jsonSettings.put("points_try", Main.match.points_try);
            jsonSettings.put("points_con", Main.match.points_con);
            jsonSettings.put("points_goal", Main.match.points_goal);
            jsonSettings.put("home_color", Main.match.home.color);
            jsonSettings.put("away_color", Main.match.away.color);
            storeFile(context, hMessage, R.string.settings_filename, jsonSettings.toString());
        }catch(Exception e){
            Log.e(Main.RRW_LOG_TAG, "FileStore.file_storeSettings Exception: " + e.getMessage());
            hMessage.sendMessage(hMessage.obtainMessage(Main.MESSAGE_TOAST, R.string.fail_store_settings));
        }
    }
    public static void readSettings(Context context, Handler hMessage){
        int help_version = 0;
        try{
            String sSettings = getFileAsString(context, hMessage, R.string.settings_filename);
            if(sSettings.length() < 3){
                hMessage.sendMessage(hMessage.obtainMessage(Main.MESSAGE_SHOW_HELP, 0));
                return;
            }
            JSONObject jsonSettings = new JSONObject(sSettings);
            hMessage.sendMessage(hMessage.obtainMessage(Main.MESSAGE_READ_SETTINGS, jsonSettings));
            if(jsonSettings.has("help_version")){
                help_version = jsonSettings.getInt("help_version");
            }
        }catch(Exception e){
            Log.e(Main.RRW_LOG_TAG, "FileStore.file_readSettings Exception: " + e.getMessage());
            hMessage.sendMessage(hMessage.obtainMessage(Main.MESSAGE_TOAST, R.string.fail_read_settings));
        }
        hMessage.sendMessage(hMessage.obtainMessage(Main.MESSAGE_SHOW_HELP, help_version));
    }

    public static void storeCustomMatchTypes(Context context, Handler hMessage){
        try{
            FileOutputStream fos = context.openFileOutput(context.getString(R.string.match_types_filename), Context.MODE_PRIVATE);
            OutputStreamWriter osr = new OutputStreamWriter(fos);
            osr.write(Conf.customMatchTypes.toString());
            osr.close();
        }catch(Exception e){
            Log.e(Main.RRW_LOG_TAG, "FileStore.file_storeCustomMatchTypes Exception: " + e.getMessage());
            hMessage.sendMessage(hMessage.obtainMessage(Main.MESSAGE_TOAST, R.string.fail_store_match_types));
        }
    }
    public static void readCustomMatchTypes(Context context, Handler hMessage){
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
            Log.i(Main.RRW_LOG_TAG, "FileStore.file_readCustomMatchTypes Match types file does not exists yet");
        }catch(Exception e){
            Log.e(Main.RRW_LOG_TAG, "FileStore.file_readCustomMatchTypes Exception: " + e.getMessage());
            hMessage.sendMessage(hMessage.obtainMessage(Main.MESSAGE_TOAST, R.string.fail_read_match_types));
        }
    }
    private static String getFileAsString(Context context, Handler hMessage, int file){
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
            Log.i(Main.RRW_LOG_TAG, "FileStore.getFileAsString File does not exist yet");
        }catch(Exception e){
            Log.e(Main.RRW_LOG_TAG, "FileStore.getFileAsString Exception: " + e.getMessage());
            hMessage.sendMessage(hMessage.obtainMessage(Main.MESSAGE_TOAST, R.string.fail_read_file));
        }
        return "";
    }
    private static void storeFile(Context context, Handler hMessage, int file, String content){
        try{
            FileOutputStream fos = context.openFileOutput(context.getString(file), Context.MODE_PRIVATE);
            OutputStreamWriter osr = new OutputStreamWriter(fos);
            osr.write(content);
            osr.close();
        }catch(Exception e){
            Log.e(Main.RRW_LOG_TAG, "FileStore.storeFile Exception: " + e.getMessage());
            hMessage.sendMessage(hMessage.obtainMessage(Main.MESSAGE_TOAST, R.string.fail_store_file));
        }
    }

}
