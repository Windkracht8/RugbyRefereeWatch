package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class history extends LinearLayout {
    private LinearLayout llMatches;
    private Button bDelete;
    final String filename = "matches.json";
    ArrayList<JSONObject> matches;

    public history(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater != null) {
            inflater.inflate(R.layout.history, this, true);
        }

        llMatches = findViewById(R.id.llMatches);
        bDelete = findViewById(R.id.bDelete);

        bDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteSelected();
            }
        });
        matches = new ArrayList<>();
        loadMatches();
    }

    public void gotMatches(final JSONArray newmatches){
        try {
            for (int i = 0; i < newmatches.length(); i++) {
                JSONObject newmatch = new JSONObject(newmatches.getString(i));
                insertMatch(newmatch);
            }
        } catch (Exception e) {
            Log.e("history", "gotMatches: " + e.getMessage());
        }
        showMatches();
        storeMatches();
    }
    private void insertMatch(JSONObject newmatch){
        try {
            long newmatchid = newmatch.getLong("matchid");
            for (int i = 0; i < matches.size(); i++) {
                long matchid = matches.get(i).getLong("matchid");
                if(newmatchid < matchid){
                    matches.add(i, newmatch);
                    return;
                }
                if(newmatchid == matchid){
                    return;
                }
            }
            matches.add(newmatch);
        } catch (Exception e) {
            Log.e("history", "insertMatch: " + e.getMessage());
        }
    }

    private void loadMatches() {
        try {
            FileInputStream fis = getContext().openFileInput(filename);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);

            StringBuilder text = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
            }
            br.close();
            String sMatches = text.toString();
            JSONArray jsonMatches = new JSONArray(sMatches);
            for (int i = 0; i < jsonMatches.length(); i++) {
                matches.add(jsonMatches.getJSONObject(i));
            }
        } catch (Exception e) {
            Log.e("history", "loadMatches: " + e.getMessage());
        }

        showMatches();
    }

    private void showMatches(){
        try {
            if(llMatches.getChildCount() > 0)
                llMatches.removeAllViews();
            Context context = getContext();
            for (int i = 0; i < matches.size(); i++) {
                llMatches.addView(new history_match(context, matches.get(i), this));
            }
        }catch (Exception e){
            Log.e("history", "showMatches: " + e.getMessage());
        }
    }

    public void storeMatches(){
        try {
            JSONArray jsonaMatches = new JSONArray();
            for (int i=0; i < matches.size(); i++) {
                jsonaMatches.put(matches.get(i));
            }
            FileOutputStream fos = getContext().openFileOutput(filename, Context.MODE_PRIVATE);
            OutputStreamWriter osr = new OutputStreamWriter(fos);
            osr.write(jsonaMatches.toString());
            osr.close();
        } catch (Exception e) {
            Log.e("history", "storeMatches: " + e.getMessage());
        }
    }

    public void unselect(){
        for (int i=0; i < llMatches.getChildCount(); i++) {
            View child = llMatches.getChildAt(i);
            if(child.getClass().getSimpleName().equals("history_match")){
                history_match tmp = (history_match)child;
                tmp.unselect();
            }
        }
    }
    public void deleteSelected(){
        for (int i=llMatches.getChildCount()-1; i >=0; i--) {
            View child = llMatches.getChildAt(i);
            if(child.getClass().getSimpleName().equals("history_match")){
                history_match tmp = (history_match)child;
                if(tmp.isselected){
                    Log.i("history", "delete match: " + i);
                    matches.remove(i);
                    llMatches.removeViewAt(i);
                }
            }
        }
        storeMatches();
        selectionChanged();
    }
    public void selectionChanged(){
        bDelete.setVisibility(View.GONE);
        for (int i=0; i < llMatches.getChildCount(); i++) {
            View child = llMatches.getChildAt(i);
            if(child.getClass().getSimpleName().equals("history_match")){
                history_match tmp = (history_match)child;
                if(tmp.isselected){
                    bDelete.setVisibility(View.VISIBLE);
                    return;
                }
            }
        }
    }
    public void updateCardReason(String reason, long matchid, long eventid){
        try {
            for (int i = 0; i < matches.size(); i++) {
                JSONObject match = matches.get(i);
                if(match.getLong("matchid") == matchid){
                    JSONArray events = match.getJSONArray("events");
                    for (int j = 0; j < events.length(); j++) {
                        JSONObject event = events.getJSONObject(j);
                        if(event.getLong("id") == eventid){
                            event.put("reason", reason);
                            storeMatches();
                            return;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("history", "updateCardReason: " + e.getMessage());
        }
    }

    public void updateTeamName(String name, String teamid, long matchid){
        try {
            for (int i = 0; i < matches.size(); i++) {
                JSONObject match = matches.get(i);
                if(match.getLong("matchid") == matchid){
                    JSONObject team = match.getJSONObject(teamid);
                    team.put("team", name);
                    storeMatches();
                    return;
                }
            }
        } catch (Exception e) {
            Log.e("history", "updateTeamName: " + e.getMessage());
        }
    }
}
