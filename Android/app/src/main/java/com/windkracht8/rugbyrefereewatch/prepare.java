package com.windkracht8.rugbyrefereewatch;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.LinearLayout;

import org.json.JSONObject;

public class prepare extends LinearLayout {
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

        String[] aMatchTypes = new String[] {"15s", "10s", "7s", "beach 7s", "beach 5s"};
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
            settings.put("record_player", cbRecordPlayer.isChecked() ? 1 : 0);
        }catch(Exception e){
            //TODO: handle error
            return null;
        }
        return settings;
    }
}
