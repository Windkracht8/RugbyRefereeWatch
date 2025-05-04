package com.windkracht8.rugbyrefereewatch;

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
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

//Thread: All of FileStore runs on a background thread
class FileStore{
    static final JSONArray customMatchTypes = new JSONArray();

    //Append a new match
    static void storeMatch(Context context){
        JSONArray matches = readMatches(context);
        matches.put(Main.match.toJson(context));
        storeMatches(context, matches);
    }

    //Store all matches
    private static void storeMatches(Context context, JSONArray matches){
        try{
            storeFile(context, R.string.matches_filename, matches.toString());
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "FileStore.storeMatches Exception: " + e.getMessage());
            Toast.makeText(context, R.string.fail_save_matches, Toast.LENGTH_SHORT).show();
        }
    }
    //Return an array of stored matches
    static JSONArray readMatches(Context context){
        try{
            String sMatches = getFileAsString(context, R.string.matches_filename);
            if(sMatches.length() < 3){return new JSONArray();}
            return new JSONArray(sMatches);
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "FileStore.readMatches Exception: " + e.getMessage());
            Toast.makeText(context, R.string.fail_read_matches, Toast.LENGTH_SHORT).show();
        }
        return new JSONArray();
    }

    //Go through stored matches and remove old ones
    static void cleanMatches(Context context){
        try{
            JSONArray matches = readMatches(context);
            for(int i = matches.length(); i > 0; i--){
                JSONObject match = matches.getJSONObject(i-1);
                if(match.getLong("matchid") < System.currentTimeMillis() - 2592000000L){
                    matches.remove(i-1);
                }
            }
            storeMatches(context, matches);
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "FileStore.cleanMatches Exception: " + e.getMessage());
            Toast.makeText(context, R.string.fail_clean_matches, Toast.LENGTH_SHORT).show();
        }
    }
    //The phone sends a list of matches that can be deleted
    static JSONArray deletedMatches(Context context, String request_data){
        try{
            JSONObject request_data_jo = new JSONObject(request_data);
            JSONArray deleted_matches = request_data_jo.getJSONArray("deleted_matches");

            JSONArray matches = readMatches(context);
            for(int d = 0; d < deleted_matches.length(); d++){
                for(int m = matches.length()-1; m >= 0; m--){
                    JSONObject match = matches.getJSONObject(m);
                    if(match.getLong("matchid") == deleted_matches.getLong(d)){
                        matches.remove(m);
                    }
                }
            }
            storeMatches(context, matches);
            return matches;
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "FileStore.deletedMatches Exception: " + e.getMessage());
            Toast.makeText(context, R.string.fail_del_matches, Toast.LENGTH_SHORT).show();
        }
        return new JSONArray();
    }
    static void storeSettings(Context context){
        try{
            JSONObject settings = Main.getSettings();
            if(settings == null){
                Log.e(Main.LOG_TAG, "FileStore.storeSettings: Main.getSettings == null");
                Toast.makeText(context, R.string.fail_store_settings, Toast.LENGTH_SHORT).show();
                return;
            }
            storeFile(context, R.string.settings_filename, settings.toString());
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "FileStore.storeSettings Exception: " + e.getMessage());
            Toast.makeText(context, R.string.fail_store_settings, Toast.LENGTH_SHORT).show();
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
            Toast.makeText(main, R.string.fail_read_settings, Toast.LENGTH_SHORT).show();
        }
    }

    private static void storeCustomMatchTypes(Context context){
        try{
            FileOutputStream fos = context.openFileOutput(context.getString(R.string.match_types_filename), Context.MODE_PRIVATE);
            OutputStreamWriter osr = new OutputStreamWriter(fos);
            osr.write(customMatchTypes.toString());
            osr.close();
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "FileStore.storeCustomMatchTypes Exception: " + e.getMessage());
            Toast.makeText(context, R.string.fail_store_match_types, Toast.LENGTH_SHORT).show();
        }
    }
    static void readCustomMatchTypes(Context context){
        try{
            FileInputStream fis = context.openFileInput(context.getString(R.string.match_types_filename));
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder text = new StringBuilder();
            String line;
            while((line = br.readLine()) != null) text.append(line);
            br.close();
            String sMatches = text.toString();
            JSONArray jsonMatchTypes = new JSONArray(sMatches);
            for(int i=customMatchTypes.length()-1; i>=0; i--)
                customMatchTypes.remove(i);
            for(int i=0; i<jsonMatchTypes.length(); i++)
                customMatchTypes.put(jsonMatchTypes.getJSONObject(i));
        }catch(FileNotFoundException e){
            Log.d(Main.LOG_TAG, "FileStore.readCustomMatchTypes Match types file does not exists yet");
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "FileStore.readCustomMatchTypes Exception: " + e.getMessage());
            Toast.makeText(context, R.string.fail_read_match_types, Toast.LENGTH_SHORT).show();
        }
    }
    static void syncCustomMatchTypes(Context context, String request_data){//Thread: Always on background thread
        try{
            JSONObject request_data_jo = new JSONObject(request_data);
            if(!request_data_jo.has("custom_match_types")) return;
            JSONArray customMatchTypes_phone = request_data_jo.getJSONArray("custom_match_types");
            for(int i=customMatchTypes.length()-1; i>=0; i--)
                customMatchTypes.remove(i);
            for(int i=0; i<customMatchTypes_phone.length(); i++)
                customMatchTypes.put(customMatchTypes_phone.getJSONObject(i));
            storeCustomMatchTypes(context);
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "FileStore.syncCustomMatchTypes Exception: " + e.getMessage());
            Toast.makeText(context, R.string.fail_sync_match_types, Toast.LENGTH_SHORT).show();
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
            Log.i(Main.LOG_TAG, "FileStore.getFileAsString File does not exist yet");
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "FileStore.getFileAsString Exception: " + e.getMessage());
            Toast.makeText(context, R.string.fail_read_file, Toast.LENGTH_SHORT).show();
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
            Log.e(Main.LOG_TAG, "FileStore.storeFile Exception: " + e.getMessage());
            Toast.makeText(context, R.string.fail_store_file, Toast.LENGTH_SHORT).show();
        }
    }

}
