package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
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
    public static void storeMatch(Main main, MatchData match){
        JSONArray matches = readMatches(main);
        matches.put(match.toJson(main));
        storeMatches(main, matches);
    }

    //Store all matches
    public static void storeMatches(Main main, JSONArray matches){
        try{
            storeFile(main, R.string.matches_filename, matches.toString());
        }catch(Exception e){
            Log.e(Main.RRW_LOG_TAG, "FileStore.file_storeMatches Exception: " + e.getMessage());
            main.handler_message.sendMessage(main.handler_message.obtainMessage(Main.MESSAGE_TOAST, R.string.fail_save_matches));
        }
    }
    //Return an array of stored matches
    public static JSONArray readMatches(Main main){
        try{
            String sMatches = getFileAsString(main, R.string.matches_filename);
            if(sMatches.length() < 3){return new JSONArray();}
            return new JSONArray(sMatches);
        }catch(Exception e){
            Log.e(Main.RRW_LOG_TAG, "FileStore.file_readMatches Exception: " + e.getMessage());
            main.handler_message.sendMessage(main.handler_message.obtainMessage(Main.MESSAGE_TOAST, R.string.fail_read_matches));
        }
        return new JSONArray();
    }

    //Go through stored matches and remove old ones
    public static void cleanMatches(Main main){
        JSONArray matches = readMatches(main);
        try{
            for(int i = matches.length(); i > 0; i--){
                JSONObject match = matches.getJSONObject(i-1);
                if(match.getLong("matchid") < Main.getCurrentTimestamp() - 1209600000){
                    matches.remove(i-1);
                }
            }
            storeMatches(main, matches);
        }catch(Exception e){
            Log.e(Main.RRW_LOG_TAG, "FileStore.file_cleanMatches Exception: " + e.getMessage());
            main.handler_message.sendMessage(main.handler_message.obtainMessage(Main.MESSAGE_TOAST, R.string.fail_clean_matches));
        }
    }
    //The phone sends a list of matches that can be deleted
    public static JSONArray deletedMatches(Main main, String request_data){
        try{
            JSONObject request_data_jo = new JSONObject(request_data);
            JSONArray deleted_matches = request_data_jo.getJSONArray("deleted_matches");

            JSONArray matches = readMatches(main);
            for(int d = 0; d < deleted_matches.length(); d++){
                for(int m = matches.length()-1; m >= 0; m--){
                    JSONObject match = matches.getJSONObject(m);
                    if(match.getLong("matchid") == deleted_matches.getLong(d)){
                        matches.remove(m);
                    }
                }
            }
            storeMatches(main, matches);
            return matches;
        }catch(Exception e){
            Log.e(Main.RRW_LOG_TAG, "FileStore.file_deletedMatches Exception: " + e.getMessage());
            main.handler_message.sendMessage(main.handler_message.obtainMessage(Main.MESSAGE_TOAST, R.string.fail_del_matches));
        }
        return new JSONArray();
    }
    public static void storeSettings(Main main){
        try{
            JSONObject settings = Main.getSettings();
            if(settings == null){
                main.handler_message.sendMessage(main.handler_message.obtainMessage(Main.MESSAGE_TOAST, R.string.fail_store_settings));
                return;
            }
            storeFile(main, R.string.settings_filename, settings.toString());
        }catch(Exception e){
            Log.e(Main.RRW_LOG_TAG, "FileStore.file_storeSettings Exception: " + e.getMessage());
            main.handler_message.sendMessage(main.handler_message.obtainMessage(Main.MESSAGE_TOAST, R.string.fail_store_settings));
        }
    }
    public static void readSettings(Main main){
        int help_version = 0;
        try{
            String sSettings = getFileAsString(main, R.string.settings_filename);
            if(sSettings.length() < 3){
                main.handler_message.sendMessage(main.handler_message.obtainMessage(Main.MESSAGE_NO_SETTINGS));
                return;
            }
            JSONObject jsonSettings = new JSONObject(sSettings);
            main.handler_message.sendMessage(main.handler_message.obtainMessage(Main.MESSAGE_READ_SETTINGS, jsonSettings));
            if(jsonSettings.has("help_version")){
                help_version = jsonSettings.getInt("help_version");
            }
        }catch(Exception e){
            Log.e(Main.RRW_LOG_TAG, "FileStore.file_readSettings Exception: " + e.getMessage());
            main.handler_message.sendMessage(main.handler_message.obtainMessage(Main.MESSAGE_TOAST, R.string.fail_read_settings));
            main.handler_message.sendMessage(main.handler_message.obtainMessage(Main.MESSAGE_NO_SETTINGS));
            return;
        }
        main.handler_message.sendMessage(main.handler_message.obtainMessage(Main.MESSAGE_SHOW_HELP, help_version, 0));
    }

    public static void storeCustomMatchTypes(Main main){
        try{
            FileOutputStream fos = main.openFileOutput(main.getString(R.string.match_types_filename), Context.MODE_PRIVATE);
            OutputStreamWriter osr = new OutputStreamWriter(fos);
            osr.write(Conf.customMatchTypes.toString());
            osr.close();
        }catch(Exception e){
            Log.e(Main.RRW_LOG_TAG, "FileStore.file_storeCustomMatchTypes Exception: " + e.getMessage());
            main.handler_message.sendMessage(main.handler_message.obtainMessage(Main.MESSAGE_TOAST, R.string.fail_store_match_types));
        }
    }
    public static void readCustomMatchTypes(Main main){
        try{
            FileInputStream fis = main.openFileInput(main.getString(R.string.match_types_filename));
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
            Log.d(Main.RRW_LOG_TAG, "FileStore.file_readCustomMatchTypes Match types file does not exists yet");
        }catch(Exception e){
            Log.e(Main.RRW_LOG_TAG, "FileStore.file_readCustomMatchTypes Exception: " + e.getMessage());
            main.handler_message.sendMessage(main.handler_message.obtainMessage(Main.MESSAGE_TOAST, R.string.fail_read_match_types));
        }
    }
    private static String getFileAsString(Main main, int file){
        try{
            FileInputStream fis = main.openFileInput(main.getString(file));
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
            main.handler_message.sendMessage(main.handler_message.obtainMessage(Main.MESSAGE_TOAST, R.string.fail_read_file));
        }
        return "";
    }
    private static void storeFile(Main main, int file, String content){
        try{
            FileOutputStream fos = main.openFileOutput(main.getString(file), Context.MODE_PRIVATE);
            OutputStreamWriter osr = new OutputStreamWriter(fos);
            osr.write(content);
            osr.close();
        }catch(Exception e){
            Log.e(Main.RRW_LOG_TAG, "FileStore.storeFile Exception: " + e.getMessage());
            main.handler_message.sendMessage(main.handler_message.obtainMessage(Main.MESSAGE_TOAST, R.string.fail_store_file));
        }
    }

}
