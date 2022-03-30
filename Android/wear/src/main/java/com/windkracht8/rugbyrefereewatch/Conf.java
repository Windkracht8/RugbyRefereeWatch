package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

public class Conf extends ScrollView{
    private Spinner color_home;
    private Spinner color_away;
    private Spinner match_type;
    private Spinner period_time;
    private Spinner period_count;
    private Spinner sinbin;
    private Spinner points_try;
    private Spinner points_con;
    private Spinner points_goal;
    private SwitchCompat record_player;
    private SwitchCompat screen_on;
    private Spinner timer_type;
    public static JSONArray customMatchTypes;
    private static final String[] aMatchTypes = new String[] {"15s", "10s", "7s", "beach 7s", "beach 5s", "custom"};

    private boolean onlyWatchSettings = false;

    public Conf(Context context, AttributeSet attrs){
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(inflater == null){Toast.makeText(context, "Failed to show correction screen", Toast.LENGTH_SHORT).show(); return;}
        inflater.inflate(R.layout.conf, this, true);

        customMatchTypes = new JSONArray();

        color_home = findViewById(R.id.color_home);
        color_away = findViewById(R.id.color_away);
        match_type = findViewById(R.id.match_type);
        period_time = findViewById(R.id.period_time);
        period_count = findViewById(R.id.period_count);
        sinbin = findViewById(R.id.sinbin);
        points_try = findViewById(R.id.points_try);
        points_con = findViewById(R.id.points_con);
        points_goal = findViewById(R.id.points_goal);
        record_player = findViewById(R.id.record_player);
        screen_on = findViewById(R.id.screen_on);
        timer_type = findViewById(R.id.timer_type);

        String[] aTemp = new String[] {"15s", "10s", "7s", "beach 7s", "beach 5s", "custom"};
        ArrayAdapter<String> aaTemp = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, aTemp);
        aaTemp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        match_type.setAdapter(aaTemp);
        match_type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id){
                switch(position){
                    case 0://15s
                        period_time.setSelection(39);
                        period_count.setSelection(1);
                        sinbin.setSelection(9);
                        points_try.setSelection(5);
                        points_con.setSelection(2);
                        points_goal.setSelection(3);
                        break;
                    case 1://10s
                        period_time.setSelection(9);
                        period_count.setSelection(1);
                        sinbin.setSelection(1);
                        points_try.setSelection(5);
                        points_con.setSelection(2);
                        points_goal.setSelection(3);
                        break;
                    case 2://7s
                        period_time.setSelection(6);
                        period_count.setSelection(1);
                        sinbin.setSelection(1);
                        points_try.setSelection(5);
                        points_con.setSelection(2);
                        points_goal.setSelection(3);
                        break;
                    case 3://beach 7s
                        period_time.setSelection(6);
                        period_count.setSelection(1);
                        sinbin.setSelection(1);
                        points_try.setSelection(1);
                        points_con.setSelection(0);
                        points_goal.setSelection(0);
                        break;
                    case 4://beach 5s
                        period_time.setSelection(4);
                        period_count.setSelection(1);
                        sinbin.setSelection(1);
                        points_try.setSelection(1);
                        points_con.setSelection(0);
                        points_goal.setSelection(0);
                        break;
                    case 5://custom
                        findViewById(R.id.custom_match).setVisibility(View.VISIBLE);
                        break;
                    default://custom match type
                        loadCustomMatchType(match_type.getSelectedItem().toString());
                }
                if(position != 5){
                    findViewById(R.id.custom_match).setVisibility(View.GONE);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView){}
        });
        aTemp = new String[] {"1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16","17","18","19","20","21","22","23","24","25","26","27","28","29","30","31","32","33","34","35","36","37","38","39","40","41","42","43","44","45","46","47","48","49","50"};
        aaTemp = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, aTemp);
        aaTemp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        period_time.setAdapter(aaTemp);
        aTemp = new String[] {"1","2","3","4","5"};
        aaTemp = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, aTemp);
        aaTemp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        period_count.setAdapter(aaTemp);
        aTemp = new String[] {"1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16","17","18","19","20"};
        aaTemp = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, aTemp);
        aaTemp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sinbin.setAdapter(aaTemp);
        aTemp = new String[] {"0","1","2","3","4","5","6","7","8","9"};
        aaTemp = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, aTemp);
        aaTemp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        points_try.setAdapter(aaTemp);
        points_con.setAdapter(aaTemp);
        points_goal.setAdapter(aaTemp);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context, R.array.team_colors, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        color_home.setAdapter(adapter);
        color_away.setAdapter(adapter);

        aTemp = new String[] {"count up", "count down"};
        aaTemp = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, aTemp);
        aaTemp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timer_type.setAdapter(aaTemp);
    }

    public void load(MatchData match){
        loadCustomMatchTypesSpinner();
        selectItem(color_home, match.home.color);
        selectItem(color_away, match.away.color);
        if(!selectItem(match_type, match.match_type)){
            //TODO: Add new custom match type
            addCustomMatchType(match);
            selectItem(match_type, match.match_type);
        }

        period_time.setSelection(match.period_time-1);
        period_count.setSelection(match.period_count-1);
        sinbin.setSelection(match.sinbin-1);
        points_try.setSelection(match.points_try);
        points_con.setSelection(match.points_con);
        points_goal.setSelection(match.points_goal);

        record_player.setChecked(MainActivity.record_player);
        screen_on.setChecked(MainActivity.screen_on);
        timer_type.setSelection(MainActivity.timer_type);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) timer_type.getLayoutParams();
        lp.setMargins(0, 0, 0, 40);
        timer_type.setLayoutParams(lp);
        findViewById(R.id.matchSettings).setVisibility(View.VISIBLE);
        findViewById(R.id.helpSettings).setVisibility(View.VISIBLE);
        onlyWatchSettings = false;
    }
    public void onlyWatchSettings(){
        findViewById(R.id.matchSettings).setVisibility(View.GONE);
        findViewById(R.id.helpSettings).setVisibility(View.GONE);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) timer_type.getLayoutParams();
        lp.setMargins(0, 0, 0, 0);
        timer_type.setLayoutParams(lp);
        onlyWatchSettings = true;
    }
    private boolean selectItem(Spinner spin, String str){
        for(int i=0;i<spin.getCount();i++){
            if(spin.getItemAtPosition(i).equals(str)){
                spin.setSelection(i);
                return true;
            }
        }
        return false;
    }
    public void save(MatchData match){
        if(!onlyWatchSettings){
            match.home.color = color_home.getSelectedItem().toString();
            match.away.color = color_away.getSelectedItem().toString();
            match.match_type = match_type.getSelectedItem().toString();

            match.period_time = period_time.getSelectedItemPosition() + 1;
            match.period_count = period_count.getSelectedItemPosition() + 1;
            match.sinbin = sinbin.getSelectedItemPosition() + 1;
            match.points_try = points_try.getSelectedItemPosition();
            match.points_con = points_con.getSelectedItemPosition();
            match.points_goal = points_goal.getSelectedItemPosition();
        }
        MainActivity.record_player = record_player.isChecked();
        MainActivity.screen_on = screen_on.isChecked();
        MainActivity.timer_type = timer_type.getSelectedItemPosition();
    }

    private void addCustomMatchType(MatchData match){
        try{
            JSONObject match_type = new JSONObject();
            match_type.put("name", match.match_type);
            match_type.put("period_time", match.period_time);
            match_type.put("period_count", match.period_count);
            match_type.put("sinbin", match.sinbin);
            match_type.put("points_try", match.points_try);
            match_type.put("points_con", match.points_con);
            match_type.put("points_goal", match.points_goal);
            customMatchTypes.put(match_type);
            loadCustomMatchTypesSpinner();

            Intent intent = new Intent("com.windkracht8.rugbyrefereewatch");
            intent.putExtra("intent_type", "storeCustomMatchTypes");
            getContext().sendBroadcast(intent);
        }catch(Exception e){
            Log.e("Conf", "addCustomMatchType: " + e.getMessage());
            Toast.makeText(getContext(), "Failed to store custom match type", Toast.LENGTH_SHORT).show();
        }
    }

    public static void syncCustomMatchTypes(Context context, String request_data){
        try{
            JSONObject request_data_jo = new JSONObject(request_data);
            if(!request_data_jo.has("custom_match_types")) return;//fine for now, probably old version of phone app
            JSONArray customMatchTypes_phone = request_data_jo.getJSONArray("custom_match_types");
            //TODO: first loop through local and remove that are no longer there

            for(int l=customMatchTypes.length()-1; l>=0; l--){
                JSONObject matchType = customMatchTypes.getJSONObject(l);
                boolean found = false;
                for (int p = 0; p < customMatchTypes_phone.length(); p++){
                    JSONObject matchType_phone = customMatchTypes_phone.getJSONObject(p);
                    if(matchType_phone.getString("name").equals(matchType.getString("name"))){
                        found = true;
                        break;
                    }
                }
                if(!found) customMatchTypes.remove(l);
            }
            for(int p=0; p < customMatchTypes_phone.length(); p++){
                JSONObject matchType_phone = customMatchTypes_phone.getJSONObject(p);
                boolean found = false;
                for(int l=0; l < customMatchTypes.length(); l++){
                    JSONObject matchType = customMatchTypes.getJSONObject(l);
                    if(matchType_phone.getString("name").equals(matchType.getString("name"))){
                        found = true;
                        matchType.put("period_time", matchType_phone.getInt("period_time"));
                        matchType.put("period_count", matchType_phone.getInt("period_count"));
                        matchType.put("sinbin", matchType_phone.getInt("sinbin"));
                        matchType.put("points_try", matchType_phone.getInt("points_try"));
                        matchType.put("points_con", matchType_phone.getInt("points_con"));
                        matchType.put("points_goal", matchType_phone.getInt("points_goal"));
                        break;
                    }
                }
                if(!found){
                    customMatchTypes.put(matchType_phone);
                }
            }
        }catch(Exception e){
            Log.e("Conf", "syncCustomMatchTypes: " + e.getMessage());
            MainActivity.makeToast(context, "Failed to sync match types");
        }
    }

    private void loadCustomMatchTypesSpinner(){
        if(customMatchTypes.length() == 0) return;
        try{
            ArrayList<String> alMatchTypes = new ArrayList<>(Arrays.asList(aMatchTypes));
            for(int i = 0; i < customMatchTypes.length(); i++){
                alMatchTypes.add(customMatchTypes.getJSONObject(i).getString("name"));
            }
            ArrayAdapter<String> aaMatchTypes = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, alMatchTypes);
            aaMatchTypes.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            match_type.setAdapter(aaMatchTypes);
        }catch(Exception e){
            Log.e("Conf", "loadCustomMatchTypesSpinner: " + e.getMessage());
            Toast.makeText(getContext(), "Failed to read custom match types from storage", Toast.LENGTH_SHORT).show();
        }
    }
    private void loadCustomMatchType(String name){
        try{
            for(int i=0; i < customMatchTypes.length(); i++){
                JSONObject matchType = customMatchTypes.getJSONObject(i);
                if(matchType.getString("name").equals(name)){
                    selectItem(period_time, matchType.getString("period_time"));
                    selectItem(period_count, matchType.getString("period_count"));
                    selectItem(sinbin, matchType.getString("sinbin"));
                    selectItem(points_try, matchType.getString("points_try"));
                    selectItem(points_con, matchType.getString("points_con"));
                    selectItem(points_goal, matchType.getString("points_goal"));
                    return;
                }
            }
        }catch(Exception e){
            Log.e("TabReport", "loadCustomMatchType: " + e.getMessage());
            Toast.makeText(getContext(), "Failed to load custom match type", Toast.LENGTH_SHORT).show();
        }
    }

}
