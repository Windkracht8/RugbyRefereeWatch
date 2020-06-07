package com.windkracht8.rugbyreferee;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
    private EditText etTimeSplit;
    private EditText etSplitCount;
    private EditText etSinbin;
    private EditText etPointsTry;
    private EditText etPointsCon;
    private EditText etPointsGoal;

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
        etTimeSplit = findViewById(R.id.etTimeSplit);
        etSplitCount = findViewById(R.id.etSplitCount);
        etSinbin = findViewById(R.id.etSinbin);
        etPointsTry = findViewById(R.id.etPointsTry);
        etPointsCon = findViewById(R.id.etPointsCon);
        etPointsGoal = findViewById(R.id.etPointsGoal);

        String[] aMatchTypes = new String[] {"15s", "10s", "7s", "beach 7s", "beach 5s"};
        ArrayAdapter<String> aaMatchTypes = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, aMatchTypes);
        aaMatchTypes.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sMatchType.setAdapter(aaMatchTypes);
        sMatchType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                switch(position){
                    case 0://15s
                        etTimeSplit.setText("40");
                        etSplitCount.setText("2");
                        etSinbin.setText("10");
                        etPointsTry.setText("5");
                        etPointsCon.setText("2");
                        etPointsGoal.setText("3");
                        break;
                    case 1://10s
                        etTimeSplit.setText("10");
                        etSplitCount.setText("2");
                        etSinbin.setText("2");
                        etPointsTry.setText("5");
                        etPointsCon.setText("2");
                        etPointsGoal.setText("3");
                        break;
                    case 2://7s
                        etTimeSplit.setText("7");
                        etSplitCount.setText("2");
                        etSinbin.setText("2");
                        etPointsTry.setText("5");
                        etPointsCon.setText("2");
                        etPointsGoal.setText("3");
                        break;
                    case 3://beach 7s
                        etTimeSplit.setText("7");
                        etSplitCount.setText("2");
                        etSinbin.setText("2");
                        etPointsTry.setText("1");
                        etPointsCon.setText("0");
                        etPointsGoal.setText("0");
                        break;
                    case 4://beach 5s
                        etTimeSplit.setText("5");
                        etSplitCount.setText("2");
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
        Log.i("prepare", "etHomeName.getHeight: " + etHomeName.getHeight());
        Log.i("prepare", "sMatchType.getHeight: " + sMatchType.getHeight());
        sMatchType.setMinimumHeight(etHomeName.getHeight());
        Log.i("prepare", "sMatchType.getHeight: " + sMatchType.getHeight());
        sHomeColor.setMinimumHeight(etHomeName.getHeight());
        sAwayColor.setMinimumHeight(etHomeName.getHeight());
    }

    public JSONObject getSettings(){
        JSONObject settings = new JSONObject();
        try {
            settings.put("home_name", etHomeName.getText());
            settings.put("away_name", etAwayName.getText());
            String temp = sHomeColor.getSelectedItem().toString();
            if(temp.equals("white"))
                temp = "lightgray";
            settings.put("home_color", temp);
            temp = sAwayColor.getSelectedItem().toString();
            if(temp.equals("white"))
                temp = "lightgray";
            settings.put("away_color", temp);
            settings.put("match_type", sMatchType.getSelectedItem().toString());
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
