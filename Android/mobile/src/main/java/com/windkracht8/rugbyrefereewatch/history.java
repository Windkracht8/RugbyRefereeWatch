package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
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

public class history extends LinearLayout{
    private LinearLayout llMatches;
    private Button bExport;
    private Button bDelete;
    private ArrayList<JSONObject> matches;
    private ArrayList<Long> deleted_matches;
    public JSONArray export_matches;
    public boolean selecting = false;

    public history(Context context, AttributeSet attrs){
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(inflater == null){Toast.makeText(context, "Failed to show history", Toast.LENGTH_SHORT).show(); return;}
        inflater.inflate(R.layout.history, this, true);

        llMatches = findViewById(R.id.llMatches);
        bExport = findViewById(R.id.bExport);
        bExport.setOnClickListener(view -> exportSelected());
        bDelete = findViewById(R.id.bDelete);
        bDelete.setOnClickListener(view -> deleteSelected());
        matches = new ArrayList<>();
        deleted_matches = new ArrayList<>();
        loadMatches();
    }

    public void gotMatches(final JSONArray matches_new){
        try{
            for(int i = 0; i < matches_new.length(); i++){
                JSONObject match_new = new JSONObject(matches_new.getString(i));
                insertMatch(match_new);
            }
        }catch(Exception e){
            Log.e("history", "gotMatches: " + e.getMessage());
            Toast.makeText(getContext(), "Failed to load history", Toast.LENGTH_SHORT).show();
        }
        showMatches();
        storeMatches();
        cleanDeletedMatches(matches_new);
    }
    private void insertMatch(final JSONObject match_new){
        try{
            long match_new_id = match_new.getLong("matchid");
            if(deleted_matches.contains(match_new_id)){
                return;
            }
            for(int i = 0; i < matches.size(); i++){
                long match_id = matches.get(i).getLong("matchid");
                if(match_new_id < match_id){
                    matches.add(i, match_new);
                    return;
                }
                if(match_new_id == match_id){
                    return;
                }
            }
            matches.add(match_new);
        }catch(Exception e){
            Log.e("history", "insertMatch: " + e.getMessage());
            Toast.makeText(getContext(), "Failed to add match to history", Toast.LENGTH_SHORT).show();
        }
    }
    public void cleanDeletedMatches(final JSONArray matches_new){
        ArrayList<Long> new_matches = new ArrayList<>();
        try{
            for(int i = 0; i < matches_new.length(); i++){
                JSONObject match_new = new JSONObject(matches_new.getString(i));
                new_matches.add(match_new.getLong("matchid"));
            }
        }catch(Exception e){
            Log.e("history", "cleanDeletedMatches: " + e.getMessage());
        }
        for(int i = deleted_matches.size()-1; i >= 0; i--){
            if(!new_matches.contains(deleted_matches.get(i))){
                deleted_matches.remove(i);
            }
        }
    }
    public JSONObject getDeletedMatches(){
        try{
            JSONArray jaDeletedMatches = new JSONArray();
            for(int i=0; i < deleted_matches.size(); i++){
                jaDeletedMatches.put(deleted_matches.get(i));
            }
            JSONObject joDeletedMatches = new JSONObject();
            joDeletedMatches.put("deleted_matches", jaDeletedMatches);
            return joDeletedMatches;
        }catch(Exception e){
            Log.e("history", "getDeletedMatches: " + e.getMessage());
        }
        return null;
    }

    private void loadMatches(){
        try{
            FileInputStream fis = getContext().openFileInput(getContext().getString(R.string.matches_filename));
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);

            StringBuilder text = new StringBuilder();
            String line;
            while((line = br.readLine()) != null){
                text.append(line);
            }
            br.close();
            String sMatches = text.toString();
            JSONArray jsonMatches = new JSONArray(sMatches);
            for(int i = 0; i < jsonMatches.length(); i++){
                matches.add(jsonMatches.getJSONObject(i));
            }
        }catch(FileNotFoundException e){
            Log.i("history", "matches file does not exists yet");
        }catch(Exception e){
            Log.e("history", "loadMatches matches: " + e.getMessage());
            Toast.makeText(getContext(), "Failed to read matches from storage", Toast.LENGTH_SHORT).show();
        }
        showMatches();

        try{
            FileInputStream fis = getContext().openFileInput(getContext().getString(R.string.deleted_matches_filename));
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);

            StringBuilder text = new StringBuilder();
            String line;
            while((line = br.readLine()) != null){
                text.append(line);
            }
            br.close();
            String sDeletedMatches = text.toString();
            JSONArray jsonDeletedMatches = new JSONArray(sDeletedMatches);
            for(int i = 0; i < jsonDeletedMatches.length(); i++){
                deleted_matches.add(jsonDeletedMatches.getLong(i));
            }
        }catch(FileNotFoundException e){
            Log.i("history", "deleted_matches file does not exists yet");
        }catch(Exception e){
            Log.e("history", "loadMatches deleted_matches: " + e.getMessage());
        }
    }
    private void showMatches(){
        try{
            if(llMatches.getChildCount() > 0) llMatches.removeAllViews();
            for(int i = 0; i < matches.size(); i++){
                llMatches.addView(new history_match(getContext(), matches.get(i), this, i == matches.size()-1));
            }
        }catch(Exception e){
            Log.e("history", "showMatches: " + e.getMessage());
            Toast.makeText(getContext(), "Failed to show history", Toast.LENGTH_SHORT).show();
        }
    }

    public void storeMatches(){
        try{
            JSONArray jaMatches = new JSONArray(matches);
            FileOutputStream fos = getContext().openFileOutput(getContext().getString(R.string.matches_filename), Context.MODE_PRIVATE);
            OutputStreamWriter osr = new OutputStreamWriter(fos);
            osr.write(jaMatches.toString());
            osr.close();
        }catch(Exception e){
            Log.e("history", "storeMatches matches: " + e.getMessage());
            Toast.makeText(getContext(), "Failed to store match", Toast.LENGTH_SHORT).show();
        }
        try{
            JSONArray jaDeletedMatches = new JSONArray();
            for(int i=0; i < deleted_matches.size(); i++){
                jaDeletedMatches.put(deleted_matches.get(i));
            }
            FileOutputStream fos = getContext().openFileOutput(getContext().getString(R.string.deleted_matches_filename), Context.MODE_PRIVATE);
            OutputStreamWriter osr = new OutputStreamWriter(fos);
            osr.write(jaDeletedMatches.toString());
            osr.close();
        }catch(Exception e){
            Log.e("history", "storeMatches deleted_matches: " + e.getMessage());
        }
    }

    public boolean unselect(){
        boolean ret = false;
        bExport.setVisibility(View.GONE);
        bDelete.setVisibility(View.GONE);
        for(int i=0; i < llMatches.getChildCount(); i++){
            View child = llMatches.getChildAt(i);
            if(child.getClass().getSimpleName().equals("history_match")){
                if(((history_match)child).unselect()) ret = true;
            }
        }
        return ret;
    }
    public void deleteSelected(){
        for(int i=llMatches.getChildCount()-1; i >=0; i--){
            View child = llMatches.getChildAt(i);
            if(child.getClass().getSimpleName().equals("history_match")){
                history_match tmp = (history_match)child;
                if(tmp.is_selected){
                    try{
                        deleted_matches.add(matches.get(i).getLong("matchid"));
                    }catch(Exception e){
                        Log.e("history", "delete match exception: " + e.getMessage());
                        Toast.makeText(getContext(), "Failed to delete match", Toast.LENGTH_SHORT).show();
                    }
                    matches.remove(i);
                    llMatches.removeViewAt(i);
                }
            }
        }
        storeMatches();
        showMatches();
        selectionChanged();
    }
    public void selectionChanged(){
        bExport.setVisibility(View.GONE);
        bDelete.setVisibility(View.GONE);
        selecting = false;
        for(int i=0; i < llMatches.getChildCount(); i++){
            View child = llMatches.getChildAt(i);
            if(child.getClass().getSimpleName().equals("history_match")){
                history_match tmp = (history_match)child;
                if(tmp.is_selected){
                    bExport.setVisibility(View.VISIBLE);
                    bDelete.setVisibility(View.VISIBLE);
                    selecting = true;
                    return;
                }
            }
        }
    }

    public void updateMatch(String sMatch){
        try{
            JSONObject match = new JSONObject(sMatch);
            long match_id = match.getLong("matchid");
            for(int i = 0; i < matches.size(); i++){
                if(matches.get(i).getLong("matchid") == match_id){
                    matches.set(i, match);
                    storeMatches();
                    showMatches();
                    return;
                }
            }
        }catch(Exception e){
            Log.e("history", "updateMatch: " + e.getMessage());
        }
        Toast.makeText(getContext(), "Failed to update match", Toast.LENGTH_SHORT).show();
    }

    public void exportSelected(){
        export_matches = new JSONArray();
        for(int i=llMatches.getChildCount()-1; i >=0; i--){
            View child = llMatches.getChildAt(i);
            if(child.getClass().getSimpleName().equals("history_match")){
                history_match tmp = (history_match)child;
                if(tmp.is_selected){
                    export_matches.put(tmp.match);
                }
            }
        }
        unselect();
        Intent intent = new Intent("com.windkracht8.rugbyrefereewatch");
        intent.putExtra("intent_type", "exportMatches");
        intent.putExtra("source", "history");
        getContext().sendBroadcast(intent);
    }
}
