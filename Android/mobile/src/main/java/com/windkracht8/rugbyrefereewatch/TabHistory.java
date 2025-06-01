/*
 * Copyright 2020-2025 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class TabHistory extends LinearLayout{
    private Main main;
    private final LinearLayout llMatches;
    private final Button bExport;
    private final Button bDelete;
    static final ArrayList<JSONObject> matches = new ArrayList<>();
    static final ArrayList<Long> deleted_matches = new ArrayList<>();
    boolean selecting = false;

    public TabHistory(Context context, AttributeSet attrs){
        super(context, attrs);
        ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.tab_history, this, true);
        llMatches = findViewById(R.id.llMatches);
        findViewById(R.id.bImport).setOnClickListener(v->main.importMatches());
        bExport = findViewById(R.id.bExport);
        bDelete = findViewById(R.id.bDelete);
        bDelete.setOnClickListener(v->deleteSelected());
    }
    void onCreateMain(Main main){
        this.main = main;
        findViewById(R.id.bSync).setOnClickListener(v->main.bSyncClick());
        findViewById(R.id.bExport).setOnClickListener(v->main.exportMatches());
        findViewById(R.id.svHistory).setOnTouchListener(main::onTouchEventScrollViews);
        findViewById(R.id.llMatches).setOnTouchListener(main::onTouchEventScrollViews);
        main.runInBackground(this::loadMatches);
    }
    void gotMatches(JSONArray matches_new){//Thread: BG
        try{
            for(int i=0; i<matches_new.length(); i++){
                insertMatch(new JSONObject(matches_new.getString(i)));
            }
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "TabHistory.gotMatches Exception: " + e.getMessage());
            main.toast(R.string.fail_receive_matches);
        }
        main.runOnUiThread(()->showMatches(true));
        storeMatches();
        cleanDeletedMatches(matches_new);
    }
    void insertMatch(JSONObject match){//Thread: BG
        try{
            long match_id = match.getLong("matchid");
            if(deleted_matches.contains(match_id)) return;
            checkFormatOfMatch(match);
            for(int i=0; i<matches.size(); i++){
                long match2_id = matches.get(i).getLong("matchid");
                if(match_id < match2_id){
                    matches.add(i, match);
                    return;
                }
                if(match_id == match2_id) return;
            }
            matches.add(match);
            storeMatches();
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "TabHistory.insertMatch Exception: " + e.getMessage());
            main.toast(R.string.fail_receive_matches);
        }
    }
    private void cleanDeletedMatches(JSONArray matches_new){//Thread: BG
        ArrayList<Long> new_matches = new ArrayList<>();
        try{
            for(int i=0; i<matches_new.length(); i++){
                JSONObject match_new = new JSONObject(matches_new.getString(i));
                new_matches.add(match_new.getLong("matchid"));
            }
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "TabHistory.cleanDeletedMatches Exception: " + e.getMessage());
        }
        for(int i=deleted_matches.size()-1; i>=0; i--){
            if(!new_matches.contains(deleted_matches.get(i))){
                deleted_matches.remove(i);
            }
        }
    }
    private void loadMatches(){//Thread: BG
        try{
            matches.clear();
            FileInputStream fis = main.openFileInput(main.getString(R.string.matches_filename));
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder text = new StringBuilder();
            String line;
            while((line = br.readLine()) != null) text.append(line);
            br.close();
            JSONArray jsonM = new JSONArray(text.toString());
            for(int i=0; i<jsonM.length(); i++) matches.add(jsonM.getJSONObject(i));
            checkFormatOfMatches();
            main.runOnUiThread(()->showMatches(true));
        }catch(FileNotFoundException e){
            storeMatches();
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "TabHistory.loadMatches matches Exception: " + e.getMessage());
            main.toast(R.string.fail_read_matches);
        }

        try{
            FileInputStream fis = main.openFileInput(main.getString(R.string.deleted_matches_filename));
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder text = new StringBuilder();
            String line;
            while((line = br.readLine()) != null) text.append(line);
            br.close();
            JSONArray jsonDM = new JSONArray(text.toString());
            for(int i=0; i<jsonDM.length(); i++) deleted_matches.add(jsonDM.getLong(i));
        }catch(FileNotFoundException e){
            storeMatches();
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "TabHistory.loadMatches deleted_matches Exception: " + e.getMessage());
        }
    }
    void showMatches(boolean showLatest){
        try{
            matches.sort((m1, m2)->{
                try{
                    return Long.compare(m1.getLong("matchid"), m2.getLong("matchid"));
                }catch(Exception e){
                    Log.e(Main.LOG_TAG, "TabHistory.showMatches Failed to sort matches " + e.getMessage());
                }
                return 0;
            });
            llMatches.removeAllViews();
            for(int i=matches.size()-1; i>=0; i--){
                llMatches.addView(new HistoryMatch(main, matches.get(i), i == 0));
            }
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "TabHistory.showMatches Exception: " + e.getMessage());
            main.toast(R.string.fail_show_history);
        }
        if(showLatest && !matches.isEmpty()) main.tabReport.loadMatch(main, matches.get(matches.size()-1));
    }
    private void storeMatches(){//Thread: BG
        try{
            JSONArray jaMatches = new JSONArray(matches);
            FileOutputStream fos = main.openFileOutput(main.getString(R.string.matches_filename), Context.MODE_PRIVATE);
            OutputStreamWriter osr = new OutputStreamWriter(fos);
            osr.write(jaMatches.toString());
            osr.close();
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "TabHistory.storeMatches matches Exception: " + e.getMessage());
            main.toast(R.string.fail_store_matches);
        }
        try{
            JSONArray jaDeletedMatches = new JSONArray();
            for(int i=0; i<deleted_matches.size(); i++){
                jaDeletedMatches.put(deleted_matches.get(i));
            }
            FileOutputStream fos = main.openFileOutput(main.getString(R.string.deleted_matches_filename), Context.MODE_PRIVATE);
            OutputStreamWriter osr = new OutputStreamWriter(fos);
            osr.write(jaDeletedMatches.toString());
            osr.close();
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "TabHistory.storeMatches deleted_matches Exception: " + e.getMessage());
        }
    }

    boolean unselect(){
        boolean ret = false;
        selecting = false;
        bExport.setVisibility(View.GONE);
        bDelete.setVisibility(View.GONE);
        for(int i=0; i<llMatches.getChildCount(); i++){
            View child = llMatches.getChildAt(i);
            if(child instanceof HistoryMatch){
                if(((HistoryMatch)child).unselect()) ret = true;
            }
        }
        return ret;
    }
    private void deleteSelected(){
        for(int i=llMatches.getChildCount()-1; i >=0; i--){
            View child = llMatches.getChildAt(i);
            if(child instanceof HistoryMatch tmp && tmp.is_selected){
                try{
                    long matchid = tmp.match.getLong("matchid");
                    deleted_matches.add(matchid);
                    for(int j=matches.size()-1; j>=0; j--){
                        if(matches.get(j).getLong("matchid") == matchid){
                            matches.remove(j);
                            break;
                        }
                    }
                    llMatches.removeViewAt(i);
                }catch(Exception e){
                    Log.e(Main.LOG_TAG, "TabHistory.deleteSelected Exception: " + e.getMessage());
                    main.toast(R.string.fail_del_match);
                }
            }
        }
        showMatches(true);
        selectionChanged();
        main.runInBackground(()->{
            storeMatches();
            main.sendSyncRequest();
        });
    }
    void selectionChanged(){
        bExport.setVisibility(View.GONE);
        bDelete.setVisibility(View.GONE);
        selecting = false;
        for(int i=0; i<llMatches.getChildCount(); i++){
            View child = llMatches.getChildAt(i);
            if(child instanceof HistoryMatch tmp && tmp.is_selected){
                bExport.setVisibility(View.VISIBLE);
                bDelete.setVisibility(View.VISIBLE);
                selecting = true;
                return;
            }
        }
    }

    void updateMatch(JSONObject match){//Thread: UI
        try{
            long match_id = match.getLong("matchid");
            for(int i=0; i<matches.size(); i++){
                if(matches.get(i).getLong("matchid") == match_id){
                    matches.set(i, match);
                    showMatches(false);
                    main.runInBackground(this::storeMatches);
                    return;
                }
            }
            if(match.has("timer")) match.remove("timer");
            matches.add(match);
            showMatches(false);
            main.runInBackground(this::storeMatches);
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "TabHistory.updateMatch Exception: " + e.getMessage());
        }
    }

    String getSelectedMatches(){
        JSONArray selected = new JSONArray();
        for(int i=llMatches.getChildCount()-1; i >=0; i--){
            View child = llMatches.getChildAt(i);
            if(child instanceof HistoryMatch tmp && tmp.is_selected){
                selected.put(tmp.match);
            }
        }
        unselect();
        return selected.toString();
    }
    private void checkFormatOfMatches(){
        try{
            matches.forEach(this::checkFormatOfMatch);
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "TabHistory.checkFormatOfMatches Exception: " + e.getMessage());
        }
    }
    private void checkFormatOfMatch(JSONObject match){
        try{
            int format = match.getInt("format");
            if(format == 3) return;
            if(format > 3){
                Toast.makeText(getContext(), R.string.update_mobile_app, Toast.LENGTH_LONG).show();
                return;
            }
            if(format < 2) updateFormat1to2(match);
            updateFormat2to3(match);
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "TabHistory.checkFormatOfMatch Exception: " + e.getMessage());
            Toast.makeText(getContext(), R.string.fail_show_match_history, Toast.LENGTH_SHORT).show();
        }
    }
    private void updateFormat1to2(JSONObject match) throws JSONException{
        //Format 2; Dec 2024; added yellow/red card count
        int home_yellow_cards = 0;
        int away_yellow_cards = 0;
        int home_red_cards = 0;
        int away_red_cards = 0;
        JSONArray events = match.getJSONArray("events");
        for(int i=0; i<events.length(); i++){
            JSONObject event = events.getJSONObject(i);
            if(event.getString("what").equals("YELLOW CARD")){
                if(event.getString("team").equals(Main.HOME_ID)){
                    home_yellow_cards++;
                }else{
                    away_yellow_cards++;
                }
            }
            if(event.getString("what").equals("RED CARD")){
                if(event.getString("team").equals(Main.HOME_ID)){
                    home_red_cards++;
                }else{
                    away_red_cards++;
                }
            }
        }
        JSONObject home = match.getJSONObject(Main.HOME_ID);
        home.put("yellow_cards", home_yellow_cards);
        home.put("red_cards", home_red_cards);
        match.put(Main.HOME_ID, home);
        JSONObject away = match.getJSONObject(Main.AWAY_ID);
        away.put("yellow_cards", away_yellow_cards);
        away.put("red_cards", away_red_cards);
        match.put(Main.AWAY_ID, away);
        match.put("format", 2);
    }
    private void updateFormat2to3(JSONObject match) throws JSONException{
        //Format 3; April 2025; change timer from ms to s
        JSONArray events = match.getJSONArray("events");
        for(int i=0; i<events.length(); i++){
            JSONObject event = events.getJSONObject(i);
            long timer = event.getLong("timer");
            event.put("timer", Math.floorDiv(timer, 1000));
        }
        match.put("format", 3);
    }
}
