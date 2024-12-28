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
    private final ArrayList<JSONObject> matches = new ArrayList<>();
    private final ArrayList<Long> deleted_matches = new ArrayList<>();
    boolean selecting = false;

    public TabHistory(Context context, AttributeSet attrs){
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        inflater.inflate(R.layout.tab_history, this, true);

        llMatches = findViewById(R.id.llMatches);
        bExport = findViewById(R.id.bExport);
        bDelete = findViewById(R.id.bDelete);
        bDelete.setOnClickListener(view -> deleteSelected());
    }
    void onCreateMain(Main main){
        this.main = main;
        findViewById(R.id.bSync).setOnClickListener(view -> main.bSyncClick());
        findViewById(R.id.bExport).setOnClickListener(view -> main.exportMatches());
        findViewById(R.id.svHistory).setOnTouchListener(main::onTouchEventScrollViews);
        findViewById(R.id.llMatches).setOnTouchListener(main::onTouchEventScrollViews);
        main.runInBackground(this::loadMatches);
    }
    void gotMatches(JSONArray matches_new){//Thread: BG
        try{
            for(int i = 0; i < matches_new.length(); i++){
                JSONObject match_new = new JSONObject(matches_new.getString(i));
                insertMatch(match_new);
            }
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "TabHistory.gotMatches Exception: " + e.getMessage());
            main.toast(R.string.fail_receive_matches);
        }
        main.runOnUiThread(this::showMatches);
        storeMatches();
        cleanDeletedMatches(matches_new);
    }
    private void insertMatch(JSONObject match){//Thread: BG
        try{
            checkFormatOfMatch(match);
            long match_id = match.getLong("matchid");
            if(deleted_matches.contains(match_id)) return;
            for(int i = 0; i < matches.size(); i++){
                long match2_id = matches.get(i).getLong("matchid");
                if(match_id < match2_id){
                    matches.add(i, match);
                    return;
                }
                if(match_id == match2_id) return;
            }
            matches.add(match);
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "TabHistory.insertMatch Exception: " + e.getMessage());
            main.toast(R.string.fail_receive_matches);
        }
    }
    private void cleanDeletedMatches(JSONArray matches_new){//Thread: BG
        ArrayList<Long> new_matches = new ArrayList<>();
        try{
            for(int i = 0; i < matches_new.length(); i++){
                JSONObject match_new = new JSONObject(matches_new.getString(i));
                new_matches.add(match_new.getLong("matchid"));
            }
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "TabHistory.cleanDeletedMatches Exception: " + e.getMessage());
        }
        for(int i = deleted_matches.size()-1; i >= 0; i--){
            if(!new_matches.contains(deleted_matches.get(i))){
                deleted_matches.remove(i);
            }
        }
    }
    JSONArray getDeletedMatches(){
        try{
            JSONArray jaDeletedMatches = new JSONArray();
            for(int i=0; i < deleted_matches.size(); i++){
                jaDeletedMatches.put(deleted_matches.get(i));
            }
            return jaDeletedMatches;
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "TabHistory.getDeletedMatches Exception: " + e.getMessage());
        }
        return null;
    }
    private void loadMatches(){//Thread: BG
        try{
            /* Testing
            //matches.add(new JSONObject("{\"matchid\":1734444251272,\"format\":1,\"settings\":{\"match_type\":\"15s\",\"period_time\":40,\"period_count\":2,\"sinbin\":10,\"points_try\":5,\"points_con\":2,\"points_goal\":3},\"home\":{\"id\":\"home\",\"team\":\"home\",\"color\":\"red\",\"tot\":5,\"tries\":1,\"cons\":0,\"pen_tries\":0,\"goals\":0,\"pens\":2,\"kickoff\":false},\"away\":{\"id\":\"away\",\"team\":\"away\",\"color\":\"blue\",\"tot\":0,\"tries\":0,\"cons\":0,\"pen_tries\":0,\"goals\":0,\"pens\":1,\"kickoff\":false},\"events\":[{\"id\":1734444251275,\"time\":\"15:04:11\",\"timer\":0,\"period\":1,\"what\":\"START\"},{\"id\":1734444252883,\"time\":\"15:04:12\",\"timer\":729,\"period\":1,\"what\":\"TRY\",\"team\":\"home\"},{\"id\":1734444257714,\"time\":\"15:04:17\",\"timer\":5729,\"period\":1,\"what\":\"YELLOW CARD\",\"team\":\"away\",\"who\":5},{\"id\":1734444259441,\"time\":\"15:04:19\",\"timer\":7730,\"period\":1,\"what\":\"PENALTY\",\"team\":\"home\"},{\"id\":1734444259652,\"time\":\"15:04:19\",\"timer\":7730,\"period\":1,\"what\":\"PENALTY\",\"team\":\"home\"},{\"id\":1734444261119,\"time\":\"15:04:21\",\"timer\":9163,\"period\":1,\"what\":\"END\",\"score\":\"5:0\"}]}"));
            //matches.add(new JSONObject("{\"matchid\":1734444402416,\"format\":1,\"settings\":{\"match_type\":\"15s\",\"period_time\":40,\"period_count\":2,\"sinbin\":10,\"points_try\":5,\"points_con\":2,\"points_goal\":3},\"home\":{\"id\":\"home\",\"team\":\"home\",\"color\":\"red\",\"tot\":0,\"tries\":0,\"cons\":0,\"pen_tries\":0,\"goals\":0,\"pens\":1,\"kickoff\":false},\"away\":{\"id\":\"away\",\"team\":\"away\",\"color\":\"blue\",\"tot\":0,\"tries\":0,\"cons\":0,\"pen_tries\":0,\"goals\":0,\"pens\":0,\"kickoff\":false},\"events\":[{\"id\":1734444402419,\"time\":\"15:06:42\",\"timer\":0,\"period\":1,\"what\":\"START\"},{\"id\":1734444405502,\"time\":\"15:06:45\",\"timer\":2586,\"period\":1,\"what\":\"PENALTY\",\"team\":\"home\"},{\"id\":1734444407231,\"time\":\"15:06:47\",\"timer\":4378,\"period\":1,\"what\":\"END\",\"score\":\"0:0\"}]}"));
            //main.runOnUiThread(this::showMatches);
             */
            FileInputStream fis = main.openFileInput(main.getString(R.string.matches_filename));
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

            checkFormatOfMatches();
            main.runOnUiThread(this::showMatches);
        }catch(FileNotFoundException e){
            Log.d(Main.LOG_TAG, "TabHistory.loadMatches Matches file does not exists yet");
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
            Log.d(Main.LOG_TAG, "TabHistory.loadMatches Deleted matches file does not exists yet");
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "TabHistory.loadMatches deleted_matches Exception: " + e.getMessage());
        }
    }
    private void showMatches(){
        try{
            matches.sort((m1, m2) -> {
                try{
                    return Long.compare(m1.getLong("matchid"), m2.getLong("matchid"));
                }catch(Exception e){
                    Log.e(Main.LOG_TAG, "TabHistory.showMatches Failed to sort matches " + e.getMessage());
                }
                return 0;
            });
            if(llMatches.getChildCount() > 0) llMatches.removeAllViews();
            for(int i = matches.size()-1; i >= 0; i--){
                llMatches.addView(new HistoryMatch(main, matches.get(i), i == 0));
            }
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "TabHistory.showMatches Exception: " + e.getMessage());
            main.toast(R.string.fail_show_history);
        }
        if(!matches.isEmpty()) main.tabReport.loadMatch(main, matches.get(matches.size()-1));
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
            for(int i=0; i < deleted_matches.size(); i++){
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
        for(int i=0; i < llMatches.getChildCount(); i++){
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
                        if(matches.get(j).getLong("matchid") == matchid) {
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
        showMatches();
        selectionChanged();
        main.runInBackground(this::storeMatches);
    }
    void selectionChanged(){
        bExport.setVisibility(View.GONE);
        bDelete.setVisibility(View.GONE);
        selecting = false;
        for(int i=0; i < llMatches.getChildCount(); i++){
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
            for(int i = 0; i < matches.size(); i++){
                if(matches.get(i).getLong("matchid") == match_id){
                    matches.set(i, match);
                    showMatches();
                    main.runInBackground(this::storeMatches);
                    return;
                }
            }
            if(match.has("timer")) match.remove("timer");
            matches.add(match);
            showMatches();
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

    private void checkFormatOfMatch(JSONObject match){
        try{
            //Format 2; Dec 2024; added yellow/red card count
            if(!match.has("format") || match.getInt("format") >= 2) return;
            int home_yellow_cards = 0;
            int away_yellow_cards = 0;
            int home_red_cards = 0;
            int away_red_cards = 0;
            JSONArray events = match.getJSONArray("events");
            for(int i = 0; i < events.length(); i++){
                JSONObject event = events.getJSONObject(i);
                if(event.getString("what").equals("YELLOW CARD")){
                    if(event.getString("team").equals("home")){
                        home_yellow_cards++;
                    }else{
                        away_yellow_cards++;
                    }
                }
                if(event.getString("what").equals("RED CARD")){
                    if(event.getString("team").equals("home")){
                        home_red_cards++;
                    }else{
                        away_red_cards++;
                    }
                }
            }
            JSONObject home = match.getJSONObject("home");
            home.put("yellow_cards", home_yellow_cards);
            home.put("red_cards", home_red_cards);
            match.put("home", home);
            JSONObject away = match.getJSONObject("away");
            away.put("yellow_cards", away_yellow_cards);
            away.put("red_cards", away_red_cards);
            match.put("away", away);
            match.put("format", 2);
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "TabHistory.checkFormatOfMatch Exception: " + e.getMessage());
        }
    }
    private void checkFormatOfMatches(){
        try{
            for(int i = 0; i < matches.size(); i++){
                JSONObject match = matches.get(i);
                if(match.has("format") && match.getInt("format") < 2){
                    checkFormatOfMatch(match);
                }
            }
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "TabHistory.checkFormatOfMatches Exception: " + e.getMessage());
        }
    }
}
