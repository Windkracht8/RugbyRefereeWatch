/*
 * Copyright 2020-2025 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.windkracht8.rugbyrefereewatch;

import android.app.AlertDialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TableLayout;
import android.widget.TextView;
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
import java.util.Arrays;

public class TabPrepare extends LinearLayout{
    private Main main;
    private final EditText etHomeName;
    private final EditText etAwayName;
    private final Spinner sHomeColor;
    private final Spinner sAwayColor;
    private final Spinner sMatchType;
    private final Button bMatchTypeDetails;
    private final TableLayout tMatchTypeDetails;
    private final EditText etPeriodTime;
    private final EditText etPeriodCount;
    private final EditText etSinbin;
    private final EditText etPointsTry;
    private final EditText etPointsCon;
    private final EditText etPointsGoal;
    private final EditText etClockPK;
    private final EditText etClockCon;
    private final EditText etClockRestart;
    private final CheckBox cbRecordPlayer;
    private final CheckBox cbRecordPens;
    private final CheckBox cbScreenOn;
    private final CheckBox cbDelayEnd;
    private final Button bDelCustom;
    private final Button bSaveCustom;
    private final Button bWatchSettings;
    private final TextView timerType;
    private boolean match_type_details = false;
    private boolean watch_settings = false;
    private boolean has_changed = false;
    static final JSONArray customMatchTypes = new JSONArray();
    private int sMatchTypePosition = 1;
    private static int sHomeColorPosition = 0;
    private static int sAwayColorPosition = 0;
    private static final int TIMER_TYPE_UP = 0;
    private static final int TIMER_TYPE_DOWN = 1;
    private static int timerTypePosition = TIMER_TYPE_DOWN;

    public TabPrepare(Context context, AttributeSet attrs){
        super(context, attrs);
        ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.tab_prepare, this, true);
        etHomeName = findViewById(R.id.etHomeName);
        sHomeColor = findViewById(R.id.sHomeColor);
        etAwayName = findViewById(R.id.etAwayName);
        sAwayColor = findViewById(R.id.sAwayColor);
        sMatchType = findViewById(R.id.sMatchType);
        bMatchTypeDetails = findViewById(R.id.bMatchTypeDetails);
        tMatchTypeDetails = findViewById(R.id.tMatchTypeDetails);
        etPeriodTime = findViewById(R.id.etPeriodTime);
        etPeriodCount = findViewById(R.id.etPeriodCount);
        etSinbin = findViewById(R.id.etSinbin);
        etPointsTry = findViewById(R.id.etPointsTry);
        etPointsCon = findViewById(R.id.etPointsCon);
        etPointsGoal = findViewById(R.id.etPointsGoal);
        etClockPK = findViewById(R.id.etClockPK);
        etClockCon = findViewById(R.id.etClockCon);
        etClockRestart = findViewById(R.id.etClockRestart);
        bDelCustom = findViewById(R.id.bDelCustom);
        bSaveCustom = findViewById(R.id.bSaveCustom);
        bWatchSettings = findViewById(R.id.bWatchSettings);
        cbScreenOn = findViewById(R.id.cbScreenOn);
        timerType = findViewById(R.id.timerType);
        cbRecordPlayer = findViewById(R.id.cbRecordPlayer);
        cbRecordPens = findViewById(R.id.cbRecordPens);
        cbDelayEnd = findViewById(R.id.cbDelayEnd);

        sMatchType.setSelection(1);
        sMatchType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override public void onItemSelected(AdapterView<?> pv, View siv, int position, long id){
                if(position == sMatchTypePosition) return;
                sMatchTypePosition = position;
                has_changed = true;
                hideMatchTypeDetails();
                bDelCustom.setVisibility(View.GONE);
                switch(position){
                    case 0://custom
                        showMatchTypeDetails();
                        break;
                    case 1://15s
                        etPeriodTime.setText(String.valueOf(40));
                        etPeriodCount.setText("2");
                        etSinbin.setText(String.valueOf(10));
                        etPointsTry.setText("5");
                        etPointsCon.setText("2");
                        etPointsGoal.setText("3");
                        etClockPK.setText(String.valueOf(60));
                        etClockCon.setText(String.valueOf(90));
                        etClockRestart.setText("0");
                        break;
                    case 2://10s
                        etPeriodTime.setText(String.valueOf(10));
                        etPeriodCount.setText("2");
                        etSinbin.setText("2");
                        etPointsTry.setText("5");
                        etPointsCon.setText("2");
                        etPointsGoal.setText("3");
                        etClockPK.setText(String.valueOf(30));
                        etClockCon.setText(String.valueOf(30));
                        etClockRestart.setText("0");
                        break;
                    case 3://7s
                        etPeriodTime.setText("7");
                        etPeriodCount.setText("2");
                        etSinbin.setText("2");
                        etPointsTry.setText("5");
                        etPointsCon.setText("2");
                        etPointsGoal.setText("3");
                        etClockPK.setText(String.valueOf(30));
                        etClockCon.setText(String.valueOf(30));
                        etClockRestart.setText(String.valueOf(30));
                        break;
                    case 4://beach 7s
                        etPeriodTime.setText("7");
                        etPeriodCount.setText("2");
                        etSinbin.setText("2");
                        etPointsTry.setText("1");
                        etPointsCon.setText("0");
                        etPointsGoal.setText("0");
                        etClockPK.setText(String.valueOf(15));
                        etClockCon.setText("0");
                        etClockRestart.setText("0");
                        break;
                    case 5://beach 5s
                        etPeriodTime.setText("5");
                        etPeriodCount.setText("2");
                        etSinbin.setText("2");
                        etPointsTry.setText("1");
                        etPointsCon.setText("0");
                        etPointsGoal.setText("0");
                        etClockPK.setText(String.valueOf(15));
                        etClockCon.setText("0");
                        etClockRestart.setText("0");
                        break;
                    default:
                        loadCustomMatchType(sMatchType.getSelectedItem().toString());
                        bDelCustom.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parentView){}
        });

        TextWatcher onNameChanged = new TextWatcher(){
            public void afterTextChanged(Editable s){}
            public void beforeTextChanged(CharSequence s, int start, int count, int after){}
            public void onTextChanged(CharSequence s, int start, int before, int count){has_changed = true;}
        };
        etHomeName.addTextChangedListener(onNameChanged);
        etAwayName.addTextChangedListener(onNameChanged);

        TextWatcher onMatchTypeDetailsChanged = new TextWatcher(){
            public void afterTextChanged(Editable s){}
            public void beforeTextChanged(CharSequence s, int start, int count, int after){}
            public void onTextChanged(CharSequence s, int start, int before, int count){
                if(match_type_details){
                    bSaveCustom.setVisibility(View.VISIBLE);
                    if(sMatchType.getSelectedItemPosition() > 0 && sMatchType.getSelectedItemPosition() < 6){
                        sMatchType.setSelection(0);
                    }
                }
            }
        };
        etPeriodTime.addTextChangedListener(onMatchTypeDetailsChanged);
        etPeriodCount.addTextChangedListener(onMatchTypeDetailsChanged);
        etSinbin.addTextChangedListener(onMatchTypeDetailsChanged);
        etPointsTry.addTextChangedListener(onMatchTypeDetailsChanged);
        etPointsCon.addTextChangedListener(onMatchTypeDetailsChanged);
        etPointsGoal.addTextChangedListener(onMatchTypeDetailsChanged);
        etClockPK.addTextChangedListener(onMatchTypeDetailsChanged);
        etClockCon.addTextChangedListener(onMatchTypeDetailsChanged);
        etClockRestart.addTextChangedListener(onMatchTypeDetailsChanged);

        String[] aTeamColors = getResources().getStringArray(R.array.teamColors);
        Arrays.sort(aTeamColors);
        ArrayAdapter<CharSequence> aaTeamColors = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, aTeamColors);
        aaTeamColors.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sHomeColor.setAdapter(aaTeamColors);
        sHomeColor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id){
                if(position != sHomeColorPosition) has_changed = true;
                Main.sharedPreferences_editor.putInt("sHomeColorPosition", position);
                Main.sharedPreferences_editor.apply();
            }
            public void onNothingSelected(AdapterView<?> adapterView){}
        });
        sAwayColor.setAdapter(aaTeamColors);
        sAwayColor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id){
                if(position != sAwayColorPosition) has_changed = true;
                Main.sharedPreferences_editor.putInt("sAwayColorPosition", position);
                Main.sharedPreferences_editor.apply();
            }
            public void onNothingSelected(AdapterView<?> adapterView){}
        });

        bMatchTypeDetails.setOnClickListener(v->bMatchTypeDetailsClick());
        bWatchSettings.setOnClickListener(v->bWatchSettingsClick());
        bSaveCustom.setOnClickListener(v->bSaveCustomClick());
        bDelCustom.setOnClickListener(v->bDelCustomClick());
        timerType.setOnClickListener(v->timerTypeClick());

        loadCustomMatchTypes();
    }

    void onCreateMain(Main main){
        this.main = main;
        findViewById(R.id.bPrepare).setOnClickListener(v->main.bPrepareClick());
        sHomeColorPosition = Main.sharedPreferences.getInt("sHomeColorPosition", 8);//Default red
        sAwayColorPosition = Main.sharedPreferences.getInt("sAwayColorPosition", 1);//Default blue
        findViewById(R.id.svPrepare).setOnTouchListener(main::onTouchEventScrollViews);
        try{
            sHomeColor.setSelection(sHomeColorPosition);
            sAwayColor.setSelection(sAwayColorPosition);
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "TabPrepare.onCreateMain: Could not set color spinner: " + e.getMessage());
        }
    }
    private void bMatchTypeDetailsClick(){
        if(match_type_details){
            hideMatchTypeDetails();
        }else{
            showMatchTypeDetails();
        }
    }
    private void showMatchTypeDetails(){
        tMatchTypeDetails.setVisibility(View.VISIBLE);
        bMatchTypeDetails.setText(R.string.match_type_details_hide);
        match_type_details = true;
    }
    private void hideMatchTypeDetails(){
        tMatchTypeDetails.setVisibility(View.GONE);
        bMatchTypeDetails.setText(R.string.match_type_details_show);
        bSaveCustom.setVisibility(View.GONE);
        match_type_details = false;
    }

    private void bWatchSettingsClick(){
        if(watch_settings){
            findViewById(R.id.tWatchSettings).setVisibility(View.GONE);
            bWatchSettings.setText(R.string.watch_settings_show);
        }else{
            if(Main.comms != null && Main.comms.status == Comms.Status.CONNECTED_IQ){
                cbScreenOn.setVisibility(View.GONE);
                findViewById(R.id.cbScreenOn_text).setVisibility(View.GONE);
            }
            findViewById(R.id.tWatchSettings).setVisibility(View.VISIBLE);
            bWatchSettings.setText(R.string.watch_settings_hide);
            //scroll down, after tWatchSettings is actually visible
            ScrollView svPrepare = findViewById(R.id.svPrepare);
            svPrepare.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener(){
                @Override public void onGlobalLayout(){
                    svPrepare.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    svPrepare.fullScroll(ScrollView.FOCUS_DOWN);
                }
            });
        }
        watch_settings = !watch_settings;
    }

    private void timerTypeClick(){
        if(timerTypePosition == TIMER_TYPE_DOWN){
            timerTypePosition = TIMER_TYPE_UP;
        }else{
            timerTypePosition = TIMER_TYPE_DOWN;
        }
        timerType.setText(timerTypePosition == TIMER_TYPE_DOWN ? R.string.timer_type_down : R.string.timer_type_up);
    }

    JSONObject getSettings(){
        if(checkSettings()) return null;
        JSONObject settings = new JSONObject();
        try{
            String home_name = String.valueOf(etHomeName.getText());
            settings.put("home_name", home_name.isEmpty() ? Main.HOME_ID : home_name);
            settings.put("home_color", Translator.getTeamColorSystem(getContext(), sHomeColor.getSelectedItem().toString()));
            String away_name = String.valueOf(etAwayName.getText());
            settings.put("away_name", away_name.isEmpty() ? Main.AWAY_ID : away_name);
            settings.put("away_color", Translator.getTeamColorSystem(getContext(), sAwayColor.getSelectedItem().toString()));
            settings.put("match_type", Translator.getMatchTypeSystem(getContext(), sMatchType.getSelectedItemPosition(), sMatchType.getSelectedItem().toString()));
            settings.put("period_time", Integer.parseInt(etPeriodTime.getText().toString()));
            settings.put("period_count", Integer.parseInt(etPeriodCount.getText().toString()));
            settings.put("sinbin", Integer.parseInt(etSinbin.getText().toString()));
            settings.put("points_try", Integer.parseInt(etPointsTry.getText().toString()));
            settings.put("points_con", Integer.parseInt(etPointsCon.getText().toString()));
            settings.put("points_goal", Integer.parseInt(etPointsGoal.getText().toString()));
            settings.put("clock_pk", Integer.parseInt(etClockPK.getText().toString()));
            settings.put("clock_con", Integer.parseInt(etClockCon.getText().toString()));
            settings.put("clock_restart", Integer.parseInt(etClockRestart.getText().toString()));
            if(watch_settings){
                settings.put("screen_on", cbScreenOn.isChecked());
                settings.put("timer_type", timerTypePosition);
                settings.put("record_player", cbRecordPlayer.isChecked());
                settings.put("record_pens", cbRecordPens.isChecked());
                settings.put("delay_end", cbDelayEnd.isChecked());
            }
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "TabPrepare.getSettings Exception: " + e.getMessage());
            Toast.makeText(getContext(), R.string.fail_prepare, Toast.LENGTH_SHORT).show();
            return null;
        }
        return settings;
    }
    void gotSettings(JSONObject settings){
        try{
            //has_changed = changes on the phone were made, don't overwrite these with settings from watch
            //watch_settings = watch settings are visible, do overwrite watch settings if they are not visible
            if(has_changed && watch_settings){return;}
            if(settings.has("screen_on")) cbScreenOn.setChecked(settings.getBoolean("screen_on"));
            if(settings.has("timer_type")){
                timerTypePosition = settings.getInt("timer_type");
                timerType.setText(timerTypePosition == TIMER_TYPE_DOWN ? R.string.timer_type_down : R.string.timer_type_up);
            }
            if(settings.has("record_player")) cbRecordPlayer.setChecked(settings.getBoolean("record_player"));
            if(settings.has("record_pens")) cbRecordPens.setChecked(settings.getBoolean("record_pens"));
            if(settings.has("delay_end")) cbDelayEnd.setChecked(settings.getBoolean("delay_end"));

            if(has_changed){return;}
            if(settings.has("home_name")){
                String home_name = settings.getString("home_name");
                etHomeName.setText(home_name.equals(Main.HOME_ID) ? "" : home_name);
            }
            if(settings.has("home_color")) Translator.setTeamColorSpin(getContext(), sHomeColor, settings.getString("home_color"));
            if(settings.has("away_name")){
                String away_name = settings.getString("away_name");
                etAwayName.setText(away_name.equals(Main.AWAY_ID) ? "" : away_name);
            }
            if(settings.has("away_color")) Translator.setTeamColorSpin(getContext(), sAwayColor, settings.getString("away_color"));

            if(settings.has("match_type")){
                int matchTypeIndex = Translator.setMatchTypeSpin(getContext(), sMatchType, settings.getString("match_type"));
                if(matchTypeIndex == 0){
                    showMatchTypeDetails();
                }else if(matchTypeIndex > 5){
                    findViewById(R.id.bDelCustom).setVisibility(View.VISIBLE);
                }
            }
            if(settings.has("period_time")) etPeriodTime.setText(String.valueOf(settings.getInt("period_time")));
            if(settings.has("period_count")) etPeriodCount.setText(String.valueOf(settings.getInt("period_count")));
            if(settings.has("sinbin")) etSinbin.setText(String.valueOf(settings.getInt("sinbin")));
            if(settings.has("points_try")) etPointsTry.setText(String.valueOf(settings.getInt("points_try")));
            if(settings.has("points_con")) etPointsCon.setText(String.valueOf(settings.getInt("points_con")));
            if(settings.has("points_goal")) etPointsGoal.setText(String.valueOf(settings.getInt("points_goal")));
            if(settings.has("clock_pk")) etClockPK.setText(String.valueOf(settings.getInt("clock_pk")));
            if(settings.has("clock_con")) etClockCon.setText(String.valueOf(settings.getInt("clock_con")));
            if(settings.has("clock_restart")) etClockRestart.setText(String.valueOf(settings.getInt("clock_restart")));
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "TabPrepare.gotSettings Exception: " + e.getMessage());
            Toast.makeText(getContext(), R.string.fail_receive_settings, Toast.LENGTH_SHORT).show();
        }
    }
    private void loadCustomMatchTypes(){
        try{
            FileInputStream fis = getContext().openFileInput(getContext().getString(R.string.match_types_filename));
            if(fis == null) return;//Probably because of viewing in IDE
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
            for(int i=0; i<jsonMatchTypes.length(); i++)
                customMatchTypes.put(jsonMatchTypes.getJSONObject(i));
            loadCustomMatchTypesSpinner();
        }catch(FileNotFoundException e){
            storeCustomMatchTypes();
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "TabPrepare.loadCustomMatchTypes Exception: " + e.getMessage());
            Toast.makeText(getContext(), R.string.fail_read_custom_match_types, Toast.LENGTH_SHORT).show();
        }
    }
    private void loadCustomMatchTypesSpinner(){
        try{
            ArrayList<String> alMatchTypes = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.matchTypes)));
            for(int i=0; i<customMatchTypes.length(); i++)
                alMatchTypes.add(customMatchTypes.getJSONObject(i).getString("name"));
            ArrayAdapter<String> aaMatchTypes = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, alMatchTypes);
            aaMatchTypes.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            sMatchType.setAdapter(aaMatchTypes);
            sMatchType.setSelection(1);
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "TabPrepare.loadCustomMatchTypesSpinner Exception: " + e.getMessage());
            Toast.makeText(getContext(), R.string.fail_read_custom_match_types, Toast.LENGTH_SHORT).show();
        }
    }
    private void loadCustomMatchType(String name){
        try{
            for(int i=0; i<customMatchTypes.length(); i++){
                JSONObject matchType = customMatchTypes.getJSONObject(i);
                if(matchType.getString("name").equals(name)){
                    etPeriodTime.setText(matchType.getString("period_time"));
                    etPeriodCount.setText(matchType.getString("period_count"));
                    etSinbin.setText(matchType.getString("sinbin"));
                    etPointsTry.setText(matchType.getString("points_try"));
                    etPointsCon.setText(matchType.getString("points_con"));
                    etPointsGoal.setText(matchType.getString("points_goal"));
                    etClockPK.setText(matchType.getString("clock_pk"));
                    etClockCon.setText(matchType.getString("clock_con"));
                    etClockRestart.setText(matchType.getString("clock_restart"));
                    break;
                }
            }
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "TabPrepare.loadCustomMatchType Exception: " + e.getMessage());
            Toast.makeText(getContext(), R.string.fail_load_custom_match_type, Toast.LENGTH_SHORT).show();
        }
    }
    private void bDelCustomClick(){
        String name = sMatchType.getSelectedItem().toString();
        try{
            for(int i=0; i<customMatchTypes.length(); i++){
                JSONObject matchType = customMatchTypes.getJSONObject(i);
                if(matchType.getString("name").equals(name)){
                    customMatchTypes.remove(i);
                    break;
                }
            }
            storeCustomMatchTypes();
            loadCustomMatchTypesSpinner();
            main.sendSyncRequest();
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "TabPrepare.bDelCustomClick Exception: " + e.getMessage());
            Toast.makeText(getContext(), R.string.fail_del_match_type, Toast.LENGTH_SHORT).show();
        }
    }
    private void bSaveCustomClick(){
        if(checkSettings()) return;
        EditText etName = new EditText(getContext());
        etName.setHint(R.string.custom_match_hint);
        etName.setMinHeight(getResources().getDimensionPixelSize(R.dimen._48dp));
        if(sMatchType.getSelectedItemPosition() > 5)
            etName.setText(sMatchType.getSelectedItem().toString());
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.save_match_type)
                .setView(etName)
                .setPositiveButton(R.string.save, (d, w)->saveCustomMatch(etName.getText().toString()))
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.show();
    }
    private void saveCustomMatch(String name){
        try{
            boolean overwrite = false;
            for(int i=0; i<customMatchTypes.length(); i++){
                JSONObject matchType = customMatchTypes.getJSONObject(i);
                if(matchType.getString("name").equals(name)){
                    overwrite = true;
                    customMatch(matchType);
                    break;
                }
            }
            if(!overwrite){
                JSONObject matchType = new JSONObject();
                matchType.put("name", name);
                customMatch(matchType);
                customMatchTypes.put(matchType);
            }
            storeCustomMatchTypes();
            loadCustomMatchTypesSpinner();
            SpinnerAdapter sa = sMatchType.getAdapter();
            for(int i=sa.getCount()-1; i>=0; i--){
                if(sa.getItem(i).toString().equals(name)){
                    sMatchType.setSelection(i);
                    break;
                }
            }
            main.sendSyncRequest();
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "TabPrepare.saveCustomMatch Exception: " + e.getMessage());
            Toast.makeText(getContext(), R.string.fail_save_match_type, Toast.LENGTH_SHORT).show();
        }
    }
    private void customMatch(JSONObject cm){
        try{
            cm.put("period_time", Integer.parseInt(etPeriodTime.getText().toString()));
            cm.put("period_count", Integer.parseInt(etPeriodCount.getText().toString()));
            cm.put("sinbin", Integer.parseInt(etSinbin.getText().toString()));
            cm.put("points_try", Integer.parseInt(etPointsTry.getText().toString()));
            cm.put("points_con", Integer.parseInt(etPointsCon.getText().toString()));
            cm.put("points_goal", Integer.parseInt(etPointsGoal.getText().toString()));
            cm.put("clock_pk", Integer.parseInt(etClockPK.getText().toString()));
            cm.put("clock_con", Integer.parseInt(etClockCon.getText().toString()));
            cm.put("clock_restart", Integer.parseInt(etClockRestart.getText().toString()));
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "TabPrepare.customMatch Exception: " + e.getMessage());
            Toast.makeText(getContext(), R.string.fail_save_match_type, Toast.LENGTH_SHORT).show();
        }
    }
    private void storeCustomMatchTypes(){
        try{
            FileOutputStream fos = getContext().openFileOutput(getContext().getString(R.string.match_types_filename), Context.MODE_PRIVATE);
            OutputStreamWriter osr = new OutputStreamWriter(fos);
            osr.write(customMatchTypes.toString());
            osr.close();
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "TabPrepare.storeCustomMatch Exception: " + e.getMessage());
            Toast.makeText(getContext(), R.string.fail_save_match_type, Toast.LENGTH_SHORT).show();
        }
    }
    private boolean checkSettings(){
        if(checkSettingsEditText(etPeriodTime, false)){
            Toast.makeText(getContext(), R.string.time_period_empty, Toast.LENGTH_SHORT).show();
            return true;
        }
        if(checkSettingsEditText(etPeriodCount, false)){
            Toast.makeText(getContext(), R.string.period_count_empty, Toast.LENGTH_SHORT).show();
            return true;
        }
        checkSettingsEditText(etSinbin, true);
        if(checkSettingsEditText(etPointsTry, false)){
            Toast.makeText(getContext(), R.string.points_try_empty, Toast.LENGTH_SHORT).show();
            return true;
        }
        checkSettingsEditText(etPointsCon, true);
        checkSettingsEditText(etPointsGoal, true);
        return false;
    }
    private boolean checkSettingsEditText(EditText check, boolean nullable){
        if(check.getText().length() < 1){
            if(nullable){
                check.setText("0");
            }else{
                return true;
            }
        }
        return false;
    }
}
