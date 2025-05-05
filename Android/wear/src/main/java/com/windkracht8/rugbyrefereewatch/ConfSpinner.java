/*
 * Copyright 2020-2025 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.windkracht8.rugbyrefereewatch;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

public class ConfSpinner extends ConfScreen{
    private final ConfItem.ConfItemType confItemType;
    ConfSpinner(ConfItem.ConfItemType confItemType){this.confItemType = confItemType;}
    @Override public @Nullable View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ){
        View rootView = super.onCreateView(inflater, container, savedInstanceState);
        label.setText(ConfItem.getConfItemName(confItemType));
        int intStart = 0;
        int intEnd = 0;
        switch(confItemType){
            case COLOR_HOME:
            case COLOR_AWAY:
                String[] teamColors = getResources().getStringArray(R.array.teamColors);
                String[] teamColors_system = getResources().getStringArray(R.array.teamColors_system);
                for(int i=0; i<teamColors_system.length; i++){
                    TextView item = addItem(teamColors[i]);
                    String teamColor_system = teamColors_system[i];
                    item.setOnClickListener(v->{
                        if(confItemType == ConfItem.ConfItemType.COLOR_HOME){
                            Main.match.home.color = teamColor_system;
                        }else{
                            Main.match.away.color = teamColor_system;
                        }
                        confActivity.goBack();
                    });
                }
                break;
            case MATCH_TYPE:
                String[] matchTypes = getResources().getStringArray(R.array.matchTypes);
                String[] matchTypes_system = getResources().getStringArray(R.array.matchTypes_system);
                for(int i = 0; i < matchTypes_system.length; i++){
                    TextView item = addItem(matchTypes[i]);
                    String matchType_system = matchTypes_system[i];
                    item.setOnClickListener(v->onMatchTypeChanged(matchType_system));
                }
                addCustomMatchTypes();
                break;
            case PERIOD_TIME:
                intStart = 1;
                intEnd = 50;
                break;
            case PERIOD_COUNT:
                intStart = 1;
                intEnd = 5;
                break;
            case SINBIN:
                intEnd = 20;
                break;
            case POINTS_TRY:
            case POINTS_CON:
            case POINTS_GOAL:
                intEnd = 9;
                break;
            case CLOCK_PK:
            case CLOCK_CON:
            case CLOCK_RESTART:
                intEnd = 90;
                break;
        }
        if(intEnd > 0){
            for(int i=intStart; i<=intEnd; i++){
                TextView item = addItem(String.valueOf(i));
                int value = i;
                item.setOnClickListener(v->{
                    switch(confItemType){
                        case PERIOD_TIME:
                            Main.match.period_time = value;
                            Main.timer_period_time = value*60;
                            break;
                        case PERIOD_COUNT:
                            Main.match.period_count = value;
                            break;
                        case SINBIN:
                            Main.match.sinbin = value;
                            break;
                        case POINTS_TRY:
                            Main.match.points_try = value;
                            break;
                        case POINTS_CON:
                            Main.match.points_con = value;
                            break;
                        case POINTS_GOAL:
                            Main.match.points_goal = value;
                            break;
                        case CLOCK_PK:
                            Main.match.clock_pk = value;
                            break;
                        case CLOCK_CON:
                            Main.match.clock_con = value;
                            break;
                        case CLOCK_RESTART:
                            Main.match.clock_restart = value;
                            break;
                    }
                    Main.match.match_type = "custom";
                    confActivity.goBack();
                });
            }
        }
        return rootView;
    }
    private void onMatchTypeChanged(String matchType){//Thread: UI
        Main.match.match_type = matchType;
        switch(matchType){
            case "custom":
                confActivity.openConfCustomScreen();
                return;
            case "15s":
                Main.match.period_time = 40;
                Main.match.period_count = 2;
                Main.match.sinbin = 10;
                Main.match.points_try = 5;
                Main.match.points_con = 2;
                Main.match.points_goal = 3;
                Main.match.clock_pk = 60;
                Main.match.clock_con = 90;
                Main.match.clock_restart = 0;
                break;
            case "10s":
                Main.match.period_time = 10;
                Main.match.period_count = 2;
                Main.match.sinbin = 2;
                Main.match.points_try = 5;
                Main.match.points_con = 2;
                Main.match.points_goal = 3;
                Main.match.clock_pk = 30;
                Main.match.clock_con = 30;
                Main.match.clock_restart = 0;
                break;
            case "7s":
                Main.match.period_time = 7;
                Main.match.period_count = 2;
                Main.match.sinbin = 2;
                Main.match.points_try = 5;
                Main.match.points_con = 2;
                Main.match.points_goal = 3;
                Main.match.clock_pk = 30;
                Main.match.clock_con = 30;
                Main.match.clock_restart = 30;
                break;
            case "beach 7s":
                Main.match.period_time = 7;
                Main.match.period_count = 2;
                Main.match.sinbin = 2;
                Main.match.points_try = 1;
                Main.match.points_con = 0;
                Main.match.points_goal = 0;
                Main.match.clock_pk = 15;
                Main.match.clock_con = 0;
                Main.match.clock_restart = 0;
                break;
            case "beach 5s":
                Main.match.period_time = 5;
                Main.match.period_count = 2;
                Main.match.sinbin = 2;
                Main.match.points_try = 1;
                Main.match.points_con = 0;
                Main.match.points_goal = 0;
                Main.match.clock_pk = 15;
                Main.match.clock_con = 0;
                Main.match.clock_restart = 0;
                break;
            default:
                loadCustomMatchType(matchType);
        }
        Main.timer_period_time = Main.match.period_time*60;
        confActivity.goBack();
    }
    private void loadCustomMatchType(String name){//Thread: UI
        try{
            for(int i=0; i<FileStore.customMatchTypes.length(); i++){
                JSONObject matchType = FileStore.customMatchTypes.getJSONObject(i);
                if(matchType.getString("name").equals(name)){
                    Main.match.period_time = matchType.getInt("period_time");
                    Main.match.period_count = matchType.getInt("period_count");
                    Main.match.sinbin = matchType.getInt("sinbin");
                    Main.match.points_try = matchType.getInt("points_try");
                    Main.match.points_con = matchType.getInt("points_con");
                    Main.match.points_goal = matchType.getInt("points_goal");
                    Main.match.clock_pk = matchType.has("clock_pk") ? matchType.getInt("clock_pk") : 0;//March 2025
                    Main.match.clock_con = matchType.has("clock_con") ? matchType.getInt("clock_con") : 0;//March 2025
                    Main.match.clock_restart = matchType.has("clock_restart") ? matchType.getInt("clock_restart") : 0;//March 2025
                    return;
                }
            }
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "ConfSpinner.loadCustomMatchType(" + name + ") Exception: " + e.getMessage());
            Toast.makeText(confActivity, R.string.fail_load_match_type, Toast.LENGTH_SHORT).show();
        }
    }
    private void addCustomMatchTypes(){
        if(FileStore.customMatchTypes.length() == 0) return;
        try{
            for(int i=0; i<FileStore.customMatchTypes.length(); i++){
                String name = FileStore.customMatchTypes.getJSONObject(i).getString("name");
                TextView item = addItem(name);
                item.setOnClickListener(v->{
                    onMatchTypeChanged(name);
                    confActivity.goBack();
                });
            }
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "ConfSpinner.addCustomMatchTypes Exception: " + e.getMessage());
            Toast.makeText(confActivity, R.string.fail_read_match_types, Toast.LENGTH_SHORT).show();
        }
    }
    private TextView addItem(String text){
        TextView item = new TextView(confActivity, null, 0, R.style.textView_item_single);
        item.setText(text);
        list.addView(item);
        confActivity.addOnTouch(item);
        return item;
    }
}
