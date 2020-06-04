package com.windkracht8.rugbyreferee;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.LinearLayout;

import org.json.JSONObject;

public class tlSettings extends LinearLayout {
    private EditText etHomeName;
    private EditText etAwayName;
    private Spinner sHomeColor;
    private Spinner sAwayColor;
    private EditText etTimeSplit;
    private EditText etSplitCount;
    private EditText etSinbin;
    private EditText etPointsTry;
    private EditText etPointsCon;
    private EditText etPointsGoal;

    public tlSettings(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        inflater.inflate(R.layout.tlsettings, this, true);

        etHomeName = findViewById(R.id.etHomeName);
        etAwayName = findViewById(R.id.etAwayName);
        sHomeColor = findViewById(R.id.sHomeColor);
        sAwayColor = findViewById(R.id.sAwayColor);
        etTimeSplit = findViewById(R.id.etTimeSplit);
        etSplitCount = findViewById(R.id.etSplitCount);
        etSinbin = findViewById(R.id.etSinbin);
        etPointsTry = findViewById(R.id.etPointsTry);
        etPointsCon = findViewById(R.id.etPointsCon);
        etPointsGoal = findViewById(R.id.etPointsGoal);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context,
                R.array.teamcolors, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sHomeColor.setAdapter(adapter);
        sAwayColor.setAdapter(adapter);

        etTimeSplit.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                int value = Integer.parseInt(s.toString());
                if(value > 50 || value < 0){
                    etTimeSplit.setBackgroundColor(0xFFFF0000);
                }else{
                    etTimeSplit.setBackgroundColor(0x00000000);
                }
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
        etSplitCount.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                int value = Integer.parseInt(s.toString());
                if(value > 5 || value < 0){
                    etSplitCount.setBackgroundColor(0xFFFF0000);
                }else{
                    etSplitCount.setBackgroundColor(0x00000000);
                }
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
        etSinbin.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                int value = Integer.parseInt(s.toString());
                if(value > 20 || value < 0){
                    etSinbin.setBackgroundColor(0xFFFF0000);
                }else{
                    etSinbin.setBackgroundColor(0x00000000);
                }
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

    }
    public void gotMatch(final JSONObject match){
        try {
            JSONObject settings = match.getJSONObject("settings");
            JSONObject home = match.getJSONObject("home");
            JSONObject away = match.getJSONObject("away");

            etHomeName.setText(home.getString("team"));
            etAwayName.setText(away.getString("team"));

            String home_color = home.getString("color");
            for (int i=0;i<sHomeColor.getCount();i++){
                if (sHomeColor.getItemAtPosition(i).equals(home_color)){
                    sHomeColor.setSelection(i);
                    break;
                }
            }
            String away_color = away.getString("color");
            for (int i=0;i<sAwayColor.getCount();i++){
                if (sAwayColor.getItemAtPosition(i).equals(away_color)){
                    sAwayColor.setSelection(i);
                    break;
                }
            }
            etTimeSplit.setText(settings.getString("split_time"));
            etSplitCount.setText(settings.getString("split_count"));
            etSinbin.setText(settings.getString("sinbin"));
            etPointsTry.setText(settings.getString("points_try"));
            etPointsCon.setText(settings.getString("points_con"));
            etPointsGoal.setText(settings.getString("points_goal"));

            this.setVisibility(View.VISIBLE);
        }catch (Exception e){
            Log.e("tlSettings", "json error: " + e.getMessage());
        }
    }
    public JSONObject getSettings(){
        JSONObject settings = new JSONObject();
        try {
            settings.put("home_name", etHomeName.getText());
            settings.put("away_name", etAwayName.getText());
            settings.put("home_color", sHomeColor.getSelectedItem().toString());
            settings.put("away_color", sAwayColor.getSelectedItem().toString());
            settings.put("split_time", Integer.parseInt(etTimeSplit.getText().toString()));
            settings.put("split_count", Integer.parseInt(etSplitCount.getText().toString()));
            settings.put("sinbin", Integer.parseInt(etSinbin.getText().toString()));
            settings.put("points_try", Integer.parseInt(etPointsTry.getText().toString()));
            settings.put("points_con", Integer.parseInt(etPointsCon.getText().toString()));
            settings.put("points_goal", Integer.parseInt(etPointsGoal.getText().toString()));
        }catch(Exception e){
            //TODO: handle error
            return null;
        }
        return settings;
    }
}
