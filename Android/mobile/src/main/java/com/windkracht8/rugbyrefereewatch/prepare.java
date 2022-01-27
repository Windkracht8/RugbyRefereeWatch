package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.json.JSONObject;

public class prepare extends LinearLayout{
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
    private CheckBox cbScreenOn;
    private Button bWatchSettings;
    private Spinner sTimerType;
    private boolean watch_settings = false;

    public prepare(Context context, AttributeSet attrs){
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(inflater == null){Toast.makeText(context, "Failed to show prepare", Toast.LENGTH_SHORT).show(); return;}
        inflater.inflate(R.layout.prepare, this, true);

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
        cbRecordPlayer = findViewById(R.id.cbRecordPlayer);
        cbScreenOn = findViewById(R.id.cbScreenOn);
        bWatchSettings = findViewById(R.id.bWatchSettings);
        sTimerType = findViewById(R.id.sTimerType);

        String[] aMatchTypes = new String[] {"15s", "10s", "7s", "beach 7s", "beach 5s", "custom"};
        ArrayAdapter<String> aaMatchTypes = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, aMatchTypes);
        aaMatchTypes.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sMatchType.setAdapter(aaMatchTypes);
        sMatchType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id){
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
                        findViewById(R.id.trTimePeriod).setVisibility(View.VISIBLE);
                        findViewById(R.id.trPeriodCount).setVisibility(View.VISIBLE);
                        findViewById(R.id.trSinbin).setVisibility(View.VISIBLE);
                        findViewById(R.id.trPointsTry).setVisibility(View.VISIBLE);
                        findViewById(R.id.trPointsCon).setVisibility(View.VISIBLE);
                        findViewById(R.id.trPointsGoal).setVisibility(View.VISIBLE);
                        break;
                }
                if(position != 5){
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

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context, R.array.team_colors, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        sHomeColor.setAdapter(adapter);
        for(int i=0;i<sHomeColor.getCount();i++){
            if(sHomeColor.getItemAtPosition(i).equals("green")){
                sHomeColor.setSelection(i);
                break;
            }
        }
        sAwayColor.setAdapter(adapter);
        for(int i=0;i<sAwayColor.getCount();i++){
            if(sAwayColor.getItemAtPosition(i).equals("red")){
                sAwayColor.setSelection(i);
                break;
            }
        }

        String[] aCountType = new String[] {"count up", "count down"};
        ArrayAdapter<String> aaCountType = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, aCountType);
        aaCountType.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sTimerType.setAdapter(aaCountType);

        bWatchSettings.setOnClickListener(view -> bWatchSettingsClick());
    }

    private void bWatchSettingsClick(){
        if(watch_settings){
            findViewById(R.id.trRecordPlayer).setVisibility(View.GONE);
            findViewById(R.id.trScreenOn).setVisibility(View.GONE);
            findViewById(R.id.trTimerType).setVisibility(View.GONE);
            bWatchSettings.setText(R.string.watch_settings);
        }else{
            findViewById(R.id.trRecordPlayer).setVisibility(View.VISIBLE);
            findViewById(R.id.trScreenOn).setVisibility(View.VISIBLE);
            findViewById(R.id.trTimerType).setVisibility(View.VISIBLE);
            bWatchSettings.setText(R.string.no_watch_settings);
        }
        watch_settings = !watch_settings;
    }
    public JSONObject getSettings(){
        JSONObject settings = new JSONObject();
        try{
            settings.put("home_name", etHomeName.getText());
            settings.put("away_name", etAwayName.getText());
            String temp = sHomeColor.getSelectedItem().toString();
            if(temp.equals("white")){temp = "lightgray";}
            settings.put("home_color", temp);
            temp = sAwayColor.getSelectedItem().toString();
            if(temp.equals("white")){temp = "lightgray";}
            settings.put("away_color", temp);
            settings.put("match_type", sMatchType.getSelectedItem().toString());
            settings.put("period_time", Integer.parseInt(etTimePeriod.getText().toString()));
            settings.put("period_count", Integer.parseInt(etPeriodCount.getText().toString()));
            settings.put("sinbin", Integer.parseInt(etSinbin.getText().toString()));
            settings.put("points_try", Integer.parseInt(etPointsTry.getText().toString()));
            settings.put("points_con", Integer.parseInt(etPointsCon.getText().toString()));
            settings.put("points_goal", Integer.parseInt(etPointsGoal.getText().toString()));
            if(watch_settings){
                settings.put("record_player", cbRecordPlayer.isChecked() ? 1 : 0);
                settings.put("screen_on", cbScreenOn.isChecked() ? 1 : 0);
                settings.put("countdown", sTimerType.getSelectedItemPosition());//DEPRECATED
                settings.put("timer_type", sTimerType.getSelectedItemPosition());
            }
        }catch(Exception e){
            Log.e("report", "getSettings: " + e.getMessage());
            Toast.makeText(getContext(), "Failed to get settings from watch", Toast.LENGTH_SHORT).show();
            return null;
        }
        return settings;
    }
    public void gotSettings(JSONObject settings){
        try{
            if(settings.has("home_name")) etHomeName.setText(settings.getString("home_name"));
            if(settings.has("home_color")) selectItem(sHomeColor, settings.getString("home_color"));
            if(settings.has("away_name")) etAwayName.setText(settings.getString("away_name"));
            if(settings.has("away_color")) selectItem(sAwayColor, settings.getString("away_color"));

            if(settings.has("match_type")) selectItem(sMatchType, settings.getString("match_type"));
            if(settings.has("period_time")) etTimePeriod.setText(String.valueOf(settings.getInt("period_time")));
            if(settings.has("period_count")) etPeriodCount.setText(String.valueOf(settings.getInt("period_count")));
            if(settings.has("sinbin")) etSinbin.setText(String.valueOf(settings.getInt("sinbin")));
            if(settings.has("points_try")) etPointsTry.setText(String.valueOf(settings.getInt("points_try")));
            if(settings.has("points_con")) etPointsCon.setText(String.valueOf(settings.getInt("points_con")));
            if(settings.has("points_goal")) etPointsGoal.setText(String.valueOf(settings.getInt("points_goal")));

            if(settings.has("record_player")) cbRecordPlayer.setChecked(settings.getInt("record_player") == 1);
            if(settings.has("screen_on")) cbScreenOn.setChecked(settings.getInt("screen_on") == 1);
            if(settings.has("timer_type")) sTimerType.setSelection(settings.getInt("timer_type"));
        }catch(Exception e){
            Log.e("Prepare", "gotSettings: " + e.getMessage());
            Toast.makeText(getContext(), "Problem with settings from watch", Toast.LENGTH_SHORT).show();
        }
    }
    private void selectItem(Spinner spin, String str){
        if(str.equals("lightgray")){str = "white";}
        for(int i=0;i<spin.getCount();i++){
            if(spin.getItemAtPosition(i).equals(str)){
                spin.setSelection(i);
                return;
            }
        }
    }
}
