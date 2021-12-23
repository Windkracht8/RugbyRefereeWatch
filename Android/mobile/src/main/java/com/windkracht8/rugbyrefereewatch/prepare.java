package com.windkracht8.rugbyrefereewatch;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.LinearLayout;

import org.json.JSONObject;

public class prepare extends LinearLayout {
    private final EditText etHomeName;
    private final EditText etAwayName;
    private final Spinner sHomeColor;
    private final Spinner sAwayColor;
    private final Spinner sMatchType;
    private final EditText etTimePeriod;
    private final EditText etPeriodCount;
    private final EditText etSinbin;
    private final EditText etPointsTry;
    private final EditText etPointsCon;
    private final EditText etPointsGoal;
    private final CheckBox cbRecordPlayer;
    private final CheckBox cbScreenOn;
    private final Button bWatchSettings;
    private final Spinner sCountDown;
    private boolean watchsettings = false;

    public prepare(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
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
        sCountDown = findViewById(R.id.sCountDown);

        String[] aMatchTypes = new String[] {"15s", "10s", "7s", "beach 7s", "beach 5s", "custom"};
        ArrayAdapter<String> aaMatchTypes = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, aMatchTypes);
        aaMatchTypes.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sMatchType.setAdapter(aaMatchTypes);
        sMatchType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                switch(position){
                    case 0://15s
                        etTimePeriod.setText("40");
                        etPeriodCount.setText("2");
                        etSinbin.setText("10");
                        etPointsTry.setText("5");
                        etPointsCon.setText("2");
                        etPointsGoal.setText("3");
                        break;
                    case 1://10s
                        etTimePeriod.setText("10");
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
            public void onNothingSelected(AdapterView<?> parentView) {}
        });

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context, R.array.teamcolors, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        sHomeColor.setAdapter(adapter);
        for (int i=0;i<sHomeColor.getCount();i++){
            if (sHomeColor.getItemAtPosition(i).equals("green")){
                sHomeColor.setSelection(i);
                break;
            }
        }
        sAwayColor.setAdapter(adapter);
        for (int i=0;i<sAwayColor.getCount();i++){
            if (sAwayColor.getItemAtPosition(i).equals("red")){
                sAwayColor.setSelection(i);
                break;
            }
        }

        String[] aCountType = new String[] {"down", "up"};
        ArrayAdapter<String> aaCountType = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, aCountType);
        aaCountType.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sCountDown.setAdapter(aaCountType);

        bWatchSettings.setOnClickListener(view -> bWatchSettingsClick());
    }

    private void bWatchSettingsClick(){
        if(watchsettings){
            findViewById(R.id.trRecordPlayer).setVisibility(View.GONE);
            findViewById(R.id.trScreenOn).setVisibility(View.GONE);
            findViewById(R.id.trCountDown).setVisibility(View.GONE);
            bWatchSettings.setText(R.string.bWatchSettings);
        }else {
            findViewById(R.id.trRecordPlayer).setVisibility(View.VISIBLE);
            findViewById(R.id.trScreenOn).setVisibility(View.VISIBLE);
            findViewById(R.id.trCountDown).setVisibility(View.VISIBLE);
            bWatchSettings.setText(R.string.bWatchSettings2);
        }
        watchsettings = !watchsettings;
    }
    public JSONObject getSettings(){
        JSONObject settings = new JSONObject();
        try {
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
            if(watchsettings) {
                settings.put("record_player", cbRecordPlayer.isChecked() ? 1 : 0);
                settings.put("screen_on", cbScreenOn.isChecked() ? 1 : 0);
                settings.put("countdown", sCountDown.getSelectedItem().toString().equals("down") ? 1 : 0);
            }
        }catch(Exception e){
            return null;
        }
        return settings;
    }
}
