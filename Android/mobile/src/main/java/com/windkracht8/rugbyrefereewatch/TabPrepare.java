package com.windkracht8.rugbyrefereewatch;

import android.app.AlertDialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.LinearLayout;
import android.widget.SpinnerAdapter;
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
    private EditText etHomeName;
    private EditText etAwayName;
    private Spinner sHomeColor;
    private Spinner sAwayColor;
    private Spinner sMatchType;
    private EditText etTimePeriod;
    private EditText etPeriodCount;
    private EditText etSinbin;
    private EditText etPointsTry;
    private EditText etPointsCon;
    private EditText etPointsGoal;
    private CheckBox cbRecordPlayer;
    private CheckBox cbRecordPens;
    private CheckBox cbScreenOn;
    private Button bWatchSettings;
    private Spinner sTimerType;
    private boolean watch_settings = false;
    private boolean has_changed = false;
    private JSONArray customMatchTypes;
    private static String[] aMatchTypes;
    private int sMatchTypePosition = 0;
    public static int sHomeColorPosition = 0;
    public static int sAwayColorPosition = 0;

    public TabPrepare(Context context, AttributeSet attrs){
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(inflater == null){Toast.makeText(context, context.getString(R.string.fail_to_show)+" "+context.getString(R.string.prepare), Toast.LENGTH_SHORT).show(); return;}
        inflater.inflate(R.layout.tab_prepare, this, true);

        customMatchTypes = new JSONArray();

        etHomeName = findViewById(R.id.etHomeName);
        etAwayName = findViewById(R.id.etAwayName);
        sHomeColor = findViewById(R.id.sHomeColor);
        sAwayColor = findViewById(R.id.sAwayColor);
        sMatchType = findViewById(R.id.sMatchType);
        etTimePeriod = findViewById(R.id.etTimePeriod);
        etPeriodCount = findViewById(R.id.etPeriodCount);
        etSinbin = findViewById(R.id.etSinbin);
        etPointsTry = findViewById(R.id.etPointsTry);
        etPointsCon = findViewById(R.id.etPointsCon);
        etPointsGoal = findViewById(R.id.etPointsGoal);
        bWatchSettings = findViewById(R.id.bWatchSettings);
        cbScreenOn = findViewById(R.id.cbScreenOn);
        sTimerType = findViewById(R.id.sTimerType);
        cbRecordPlayer = findViewById(R.id.cbRecordPlayer);
        cbRecordPens = findViewById(R.id.cbRecordPens);

        aMatchTypes = new String[] {context.getString(R.string.type_15s),
                                    context.getString(R.string.type_10s),
                                    context.getString(R.string.type_7s),
                                    context.getString(R.string.beach_7s),
                                    context.getString(R.string.beach_5s),
                                    context.getString(R.string.custom)
        };
        ArrayAdapter<String> aaMatchTypes = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, aMatchTypes);
        aaMatchTypes.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sMatchType.setAdapter(aaMatchTypes);
        sMatchType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id){
                if(position == sMatchTypePosition) return;
                sMatchTypePosition = position;
                has_changed = true;
                findViewById(R.id.trDelCustom).setVisibility(View.GONE);
                switch(position){
                    case 0://15s
                        etTimePeriod.setText(String.valueOf(40));
                        etPeriodCount.setText("2");
                        etSinbin.setText(String.valueOf(10));
                        etPointsTry.setText("5");
                        etPointsCon.setText("2");
                        etPointsGoal.setText("3");
                        break;
                    case 1://10s
                        etTimePeriod.setText(String.valueOf(10));
                        etPeriodCount.setText("2");
                        etSinbin.setText("2");
                        etPointsTry.setText("5");
                        etPointsCon.setText("2");
                        etPointsGoal.setText("3");
                        break;
                    case 2://7s
                        etTimePeriod.setText("7");
                        etPeriodCount.setText("2");
                        etSinbin.setText("2");
                        etPointsTry.setText("5");
                        etPointsCon.setText("2");
                        etPointsGoal.setText("3");
                        break;
                    case 3://beach 7s
                        etTimePeriod.setText("7");
                        etPeriodCount.setText("2");
                        etSinbin.setText("2");
                        etPointsTry.setText("1");
                        etPointsCon.setText("0");
                        etPointsGoal.setText("0");
                        break;
                    case 4://beach 5s
                        etTimePeriod.setText("5");
                        etPeriodCount.setText("2");
                        etSinbin.setText("2");
                        etPointsTry.setText("1");
                        etPointsCon.setText("0");
                        etPointsGoal.setText("0");
                        break;
                    case 5://custom
                        findViewById(R.id.trSaveCustom).setVisibility(View.VISIBLE);
                        findViewById(R.id.trTimePeriod).setVisibility(View.VISIBLE);
                        findViewById(R.id.trPeriodCount).setVisibility(View.VISIBLE);
                        findViewById(R.id.trSinbin).setVisibility(View.VISIBLE);
                        findViewById(R.id.trPointsTry).setVisibility(View.VISIBLE);
                        findViewById(R.id.trPointsCon).setVisibility(View.VISIBLE);
                        findViewById(R.id.trPointsGoal).setVisibility(View.VISIBLE);
                        break;
                    default:
                        findViewById(R.id.trDelCustom).setVisibility(View.VISIBLE);
                        findViewById(R.id.trTimePeriod).setVisibility(View.VISIBLE);
                        findViewById(R.id.trPeriodCount).setVisibility(View.VISIBLE);
                        findViewById(R.id.trSinbin).setVisibility(View.VISIBLE);
                        findViewById(R.id.trPointsTry).setVisibility(View.VISIBLE);
                        findViewById(R.id.trPointsCon).setVisibility(View.VISIBLE);
                        findViewById(R.id.trPointsGoal).setVisibility(View.VISIBLE);
                        loadCustomMatchType(sMatchType.getSelectedItem().toString());
                }
                if(position != 5){
                    findViewById(R.id.trSaveCustom).setVisibility(View.GONE);
                    findViewById(R.id.trTimePeriod).setVisibility(View.GONE);
                    findViewById(R.id.trPeriodCount).setVisibility(View.GONE);
                    findViewById(R.id.trSinbin).setVisibility(View.GONE);
                    findViewById(R.id.trPointsTry).setVisibility(View.GONE);
                    findViewById(R.id.trPointsCon).setVisibility(View.GONE);
                    findViewById(R.id.trPointsGoal).setVisibility(View.GONE);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView){}
        });

        etHomeName.addTextChangedListener(new TextWatcher(){
            public void afterTextChanged(Editable s){}
            public void beforeTextChanged(CharSequence s, int start, int count, int after){}
            public void onTextChanged(CharSequence s, int start, int before, int count){has_changed = true;}
        });
        etAwayName.addTextChangedListener(new TextWatcher(){
            public void afterTextChanged(Editable s){}
            public void beforeTextChanged(CharSequence s, int start, int count, int after){}
            public void onTextChanged(CharSequence s, int start, int before, int count){has_changed = true;}
        });

        String[] aTeamColors = {context.getString(R.string.black),
                context.getString(R.string.blue),
                context.getString(R.string.brown),
                context.getString(R.string.gold),
                context.getString(R.string.green),
                context.getString(R.string.orange),
                context.getString(R.string.pink),
                context.getString(R.string.purple),
                context.getString(R.string.red),
                context.getString(R.string.white)
        };
        Arrays.sort(aTeamColors);
        ArrayAdapter<CharSequence> aaTeamColors = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, aTeamColors);
        aaTeamColors.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        sHomeColor.setAdapter(aaTeamColors);
        sHomeColor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if(position != sHomeColorPosition) has_changed = true;
                MainActivity.sharedPreferences_editor.putInt("sHomeColorPosition", position);
                MainActivity.sharedPreferences_editor.apply();
            }
            public void onNothingSelected(AdapterView<?> adapterView){}
        });

        sAwayColor.setAdapter(aaTeamColors);
        sAwayColor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if(position != sAwayColorPosition) has_changed = true;
                MainActivity.sharedPreferences_editor.putInt("sAwayColorPosition", position);
                MainActivity.sharedPreferences_editor.apply();
            }
            public void onNothingSelected(AdapterView<?> adapterView){}
        });

        String[] aCountType = new String[] {context.getString(R.string.count_up),
                                            context.getString(R.string.count_down)};
        ArrayAdapter<String> aaCountType = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, aCountType);
        aaCountType.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sTimerType.setAdapter(aaCountType);
        sTimerType.setSelection(1);

        bWatchSettings.setOnClickListener(view -> bWatchSettingsClick());
        findViewById(R.id.bSaveCustom).setOnClickListener(view -> bSaveCustomClick());
        findViewById(R.id.bDelCustom).setOnClickListener(view -> bDelCustomClick());

        loadCustomMatchTypes();
    }

    private void bWatchSettingsClick(){
        if(watch_settings){
            findViewById(R.id.trScreenOn).setVisibility(View.GONE);
            findViewById(R.id.trTimerType).setVisibility(View.GONE);
            findViewById(R.id.trRecordPlayer).setVisibility(View.GONE);
            findViewById(R.id.trRecordPens).setVisibility(View.GONE);
            bWatchSettings.setText(R.string.watch_settings);
        }else{
            findViewById(R.id.trScreenOn).setVisibility(View.VISIBLE);
            findViewById(R.id.trTimerType).setVisibility(View.VISIBLE);
            findViewById(R.id.trRecordPlayer).setVisibility(View.VISIBLE);
            findViewById(R.id.trRecordPens).setVisibility(View.VISIBLE);
            bWatchSettings.setText(R.string.no_watch_settings);
        }
        watch_settings = !watch_settings;
    }
    public JSONObject getSettings(){
        if(checkSettings()) return null;
        JSONObject settings = new JSONObject();
        try{
            settings.put("home_name", etHomeName.getText());
            settings.put("away_name", etAwayName.getText());
            settings.put("home_color", translator.getTeamColorSystem(sHomeColor.getSelectedItem().toString()));
            settings.put("away_color", translator.getTeamColorSystem(sAwayColor.getSelectedItem().toString()));
            settings.put("match_type", translator.getMatchTypeSystem(sMatchType.getSelectedItemPosition()));
            settings.put("period_time", Integer.parseInt(etTimePeriod.getText().toString()));
            settings.put("period_count", Integer.parseInt(etPeriodCount.getText().toString()));
            settings.put("sinbin", Integer.parseInt(etSinbin.getText().toString()));
            settings.put("points_try", Integer.parseInt(etPointsTry.getText().toString()));
            settings.put("points_con", Integer.parseInt(etPointsCon.getText().toString()));
            settings.put("points_goal", Integer.parseInt(etPointsGoal.getText().toString()));
            if(watch_settings){
                settings.put("screen_on", cbScreenOn.isChecked() ? 1 : 0);
                settings.put("timer_type", sTimerType.getSelectedItemPosition());
                settings.put("record_player", cbRecordPlayer.isChecked() ? 1 : 0);
                settings.put("record_pens", cbRecordPens.isChecked() ? 1 : 0);
            }
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "TabPrepare.getSettings Exception: " + e.getMessage());
            Toast.makeText(getContext(), getContext().getString(R.string.fail_prepare), Toast.LENGTH_SHORT).show();
            return null;
        }
        return settings;
    }
    public void gotSettings(JSONObject settings){
        if(has_changed){return;}
        try{
            if(settings.has("home_name")) etHomeName.setText(settings.getString("home_name"));
            if(settings.has("home_color")) translator.setTeamColor(getContext(), sHomeColor, settings.getString("home_color"));
            if(settings.has("away_name")) etAwayName.setText(settings.getString("away_name"));
            if(settings.has("away_color")) translator.setTeamColor(getContext(), sAwayColor, settings.getString("away_color"));

            if(settings.has("match_type")) translator.setMatchType(sMatchType, settings.getString("match_type"));
            if(settings.has("period_time")) etTimePeriod.setText(String.valueOf(settings.getInt("period_time")));
            if(settings.has("period_count")) etPeriodCount.setText(String.valueOf(settings.getInt("period_count")));
            if(settings.has("sinbin")) etSinbin.setText(String.valueOf(settings.getInt("sinbin")));
            if(settings.has("points_try")) etPointsTry.setText(String.valueOf(settings.getInt("points_try")));
            if(settings.has("points_con")) etPointsCon.setText(String.valueOf(settings.getInt("points_con")));
            if(settings.has("points_goal")) etPointsGoal.setText(String.valueOf(settings.getInt("points_goal")));

            if(settings.has("screen_on")) cbScreenOn.setChecked(settings.getInt("screen_on") == 1);
            if(settings.has("timer_type")) sTimerType.setSelection(settings.getInt("timer_type"));
            if(settings.has("record_player")) cbRecordPlayer.setChecked(settings.getInt("record_player") == 1);
            if(settings.has("record_pens")) cbRecordPens.setChecked(settings.getInt("record_pens") == 1);
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "TabPrepare.gotSettings Exception: " + e.getMessage());
            Toast.makeText(getContext(), getContext().getString(R.string.fail_receive_settings), Toast.LENGTH_SHORT).show();
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
            for(int i = 0; i < jsonMatchTypes.length(); i++){
                customMatchTypes.put(jsonMatchTypes.getJSONObject(i));
            }
            loadCustomMatchTypesSpinner();
        }catch(FileNotFoundException e){
            Log.i(MainActivity.RRW_LOG_TAG, "TabPrepare.loadCustomMatchTypes Match types file does not exists yet");
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "TabPrepare.loadCustomMatchTypes Exception: " + e.getMessage());
            Toast.makeText(getContext(), getContext().getString(R.string.fail_read_custom_match_types), Toast.LENGTH_SHORT).show();
        }
    }
    private void loadCustomMatchTypesSpinner(){
        try{
            ArrayList<String> alMatchTypes = new ArrayList<>(Arrays.asList(aMatchTypes));
            for(int i = 0; i < customMatchTypes.length(); i++){
                alMatchTypes.add(customMatchTypes.getJSONObject(i).getString("name"));
            }
            ArrayAdapter<String> aaMatchTypes = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, alMatchTypes);
            aaMatchTypes.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            sMatchType.setAdapter(aaMatchTypes);
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "TabPrepare.loadCustomMatchTypesSpinner Exception: " + e.getMessage());
            Toast.makeText(getContext(), getContext().getString(R.string.fail_read_custom_match_types), Toast.LENGTH_SHORT).show();
        }
    }
    private void loadCustomMatchType(String name){
        try{
            for(int i=0; i < customMatchTypes.length(); i++){
                JSONObject matchType = customMatchTypes.getJSONObject(i);
                if(matchType.getString("name").equals(name)){
                    etTimePeriod.setText(matchType.getString("period_time"));
                    etPeriodCount.setText(matchType.getString("period_count"));
                    etSinbin.setText(matchType.getString("sinbin"));
                    etPointsTry.setText(matchType.getString("points_try"));
                    etPointsCon.setText(matchType.getString("points_con"));
                    etPointsGoal.setText(matchType.getString("points_goal"));
                }
            }
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "TabPrepare.loadCustomMatchType Exception: " + e.getMessage());
            Toast.makeText(getContext(), getContext().getString(R.string.fail_load_custom_match_type), Toast.LENGTH_SHORT).show();
        }
    }
    private void bDelCustomClick(){
        String name = sMatchType.getSelectedItem().toString();
        try{
            for(int i=0; i < customMatchTypes.length(); i++){
                JSONObject matchType = customMatchTypes.getJSONObject(i);
                if(matchType.getString("name").equals(name)){
                    customMatchTypes.remove(i);
                    break;
                }
            }
            storeCustomMatchTypes();
            loadCustomMatchTypesSpinner();
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "TabPrepare.bDelCustomClick Exception: " + e.getMessage());
            Toast.makeText(getContext(), getContext().getString(R.string.fail_del_match_type), Toast.LENGTH_SHORT).show();
        }
    }
    private void bSaveCustomClick(){
        if(checkSettings()) return;
        final EditText etName = new EditText(getContext());
        etName.setHint(R.string.custom_match_hint);
        etName.setMinimumHeight((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                                    48,
                                    etName.getResources().getDisplayMetrics())
        );
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.save_match_type)
                .setView(etName)
                .setPositiveButton(R.string.save, (dialog1, which) -> saveCustomMatch(etName.getText().toString()))
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.show();
    }
    private void saveCustomMatch(String name){
        try{
            boolean overwrite = false;
            for(int i=0; i < customMatchTypes.length(); i++){
                JSONObject matchType = customMatchTypes.getJSONObject(i);
                if(matchType.getString("name").equals(name)){
                    overwrite = true;
                    customMatch(matchType);
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
                if(sa.getItem(i).toString().equals(name)) {
                    sMatchType.setSelection(i);
             }
            }
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "TabPrepare.saveCustomMatch Exception: " + e.getMessage());
            Toast.makeText(getContext(), getContext().getString(R.string.fail_save_match_type), Toast.LENGTH_SHORT).show();
        }
    }
    private void customMatch(JSONObject cm){
        try{
            cm.put("period_time", etTimePeriod.getText().toString());
            cm.put("period_count", etPeriodCount.getText().toString());
            cm.put("sinbin", etSinbin.getText().toString());
            cm.put("points_try", etPointsTry.getText().toString());
            cm.put("points_con", etPointsCon.getText().toString());
            cm.put("points_goal", etPointsGoal.getText().toString());
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "TabPrepare.customMatch Exception: " + e.getMessage());
            Toast.makeText(getContext(), getContext().getString(R.string.fail_save_match_type), Toast.LENGTH_SHORT).show();
        }
    }
    private void storeCustomMatchTypes(){
        try{
            FileOutputStream fos = getContext().openFileOutput(getContext().getString(R.string.match_types_filename), Context.MODE_PRIVATE);
            OutputStreamWriter osr = new OutputStreamWriter(fos);
            osr.write(customMatchTypes.toString());
            osr.close();
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "TabPrepare.storeCustomMatch Exception: " + e.getMessage());
            Toast.makeText(getContext(), getContext().getString(R.string.fail_save_match_type), Toast.LENGTH_SHORT).show();
        }
    }
    private boolean checkSettings(){
        if(checkSettingsEditText(etHomeName, false)){
            Toast.makeText(getContext(), getContext().getString(R.string.home_name_empty), Toast.LENGTH_SHORT).show();
            return true;
        }
        if(checkSettingsEditText(etAwayName, false)){
            Toast.makeText(getContext(), getContext().getString(R.string.away_name_empty), Toast.LENGTH_SHORT).show();
            return true;
        }
        if(checkSettingsEditText(etTimePeriod, false)){
            Toast.makeText(getContext(), getContext().getString(R.string.time_period_empty), Toast.LENGTH_SHORT).show();
            return true;
        }
        if(checkSettingsEditText(etPeriodCount, false)){
            Toast.makeText(getContext(), getContext().getString(R.string.period_count_empty), Toast.LENGTH_SHORT).show();
            return true;
        }
        checkSettingsEditText(etSinbin, true);
        if(checkSettingsEditText(etPointsTry, false)){
            Toast.makeText(getContext(), getContext().getString(R.string.points_try_empty), Toast.LENGTH_SHORT).show();
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
    public JSONArray getCustomMatchTypes(){return customMatchTypes;}
}
