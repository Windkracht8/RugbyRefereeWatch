package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

public class MenuItem extends ConstraintLayout{
    enum MenuItemType {
        COLOR_HOME, COLOR_AWAY, MATCH_TYPE, PERIOD_TIME, PERIOD_COUNT, SINBIN, POINTS_TRY,
        POINTS_CON, POINTS_GOAL, SCREEN_ON, TIMER_TYPE, RECORD_PLAYER, RECORD_PENS, HELP
    }
    private MenuItemType type;
    private TextView menuItemName;
    private TextView menuItemValue;
    private Spinner menuItemColor;
    private Spinner menuItemMatchType;
    private Spinner menuItemNumbers;
    public MenuItem(Context context, AttributeSet attrs){super(context, attrs);}
    public MenuItem(Context context, AttributeSet attrs, MenuItemType type){
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(inflater == null){Toast.makeText(context, R.string.fail_show_conf, Toast.LENGTH_SHORT).show();return;}
        inflater.inflate(R.layout.menu_item, this, true);

        menuItemName = findViewById(R.id.menuItemName);
        menuItemValue = findViewById(R.id.menuItemValue);
        menuItemColor = findViewById(R.id.menuItemColor);
        menuItemMatchType = findViewById(R.id.menuItemMatchType);
        menuItemNumbers = findViewById(R.id.menuItemNumbers);

        this.type = type;
        String name = "";
        switch(type){
            case COLOR_HOME:
                name = context.getString(R.string.color_home);
                menuItemColor.setOnItemSelectedListener(itemSelectedListener);
                break;
            case COLOR_AWAY:
                name = context.getString(R.string.color_away);
                menuItemColor.setOnItemSelectedListener(itemSelectedListener);
                break;
            case MATCH_TYPE:
                name = context.getString(R.string.match_type);
                menuItemMatchType.setOnItemSelectedListener(itemSelectedListener);
                break;
            case PERIOD_TIME:
                this.setVisibility(View.GONE);
                name = context.getString(R.string.period_time);
                menuItemNumbers.setAdapter(getAA(new String[]{"","1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16","17","18","19","20","21","22","23","24","25","26","27","28","29","30","31","32","33","34","35","36","37","38","39","40","41","42","43","44","45","46","47","48","49","50"}));
                menuItemNumbers.setOnItemSelectedListener(itemSelectedListener);
                break;
            case PERIOD_COUNT:
                this.setVisibility(View.GONE);
                name = context.getString(R.string.period_count);
                menuItemNumbers.setAdapter(getAA(new String[]{"","1","2","3","4","5"}));
                menuItemNumbers.setOnItemSelectedListener(itemSelectedListener);
                break;
            case SINBIN:
                this.setVisibility(View.GONE);
                name = context.getString(R.string.sinbin);
                menuItemNumbers.setAdapter(getAA(new String[]{"","1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16","17","18","19","20"}));
                menuItemNumbers.setOnItemSelectedListener(itemSelectedListener);
                break;
            case POINTS_TRY:
                this.setVisibility(View.GONE);
                name = context.getString(R.string.points_try);
                menuItemNumbers.setAdapter(getAA(new String[]{"","0", "1", "2", "3", "4", "5", "6", "7", "8", "9"}));
                menuItemNumbers.setOnItemSelectedListener(itemSelectedListener);
                break;
            case POINTS_CON:
                this.setVisibility(View.GONE);
                name = context.getString(R.string.points_con);
                menuItemNumbers.setAdapter(getAA(new String[]{"","0", "1", "2", "3", "4", "5", "6", "7", "8", "9"}));
                menuItemNumbers.setOnItemSelectedListener(itemSelectedListener);
                break;
            case POINTS_GOAL:
                this.setVisibility(View.GONE);
                name = context.getString(R.string.points_goal);
                menuItemNumbers.setAdapter(getAA(new String[]{"","0", "1", "2", "3", "4", "5", "6", "7", "8", "9"}));
                menuItemNumbers.setOnItemSelectedListener(itemSelectedListener);
                break;
            case SCREEN_ON:
                name = context.getString(R.string.screen_on);
                break;
            case TIMER_TYPE:
                name = context.getString(R.string.timer_type);
                break;
            case RECORD_PLAYER:
                name = context.getString(R.string.record_player);
                break;
            case RECORD_PENS:
                name = context.getString(R.string.record_pens);
                break;
            case HELP:
                name = context.getString(R.string.help);
                menuItemValue.setVisibility(View.GONE);
                break;
        }
        menuItemName.setText(name);
        this.setOnClickListener(v -> click());
        menuItemValue.setContentDescription(context.getString(R.string.menuItemValue_desc) + type);
    }
    private ArrayAdapter<String> getAA(String[] a){
        ArrayAdapter<String> aa = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, a);
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return aa;
    }
    public void setHeight(int height){
        menuItemName.setTextSize(TypedValue.COMPLEX_UNIT_PX, height*2);
        menuItemValue.setTextSize(TypedValue.COMPLEX_UNIT_PX, height);
    }
    public void addOnTouch(MainActivity ma){
        ma.addOnTouch(this);
        ma.addOnTouch(menuItemName);
        ma.addOnTouch(menuItemValue);
    }
    @Override
    protected void onVisibilityChanged(View changedView, int visibility){
        super.onVisibilityChanged(changedView, visibility);
        if(visibility == VISIBLE){
            updateValue();
        }
    }
    private boolean hideForMatchType(){
        if(MainActivity.match.match_type.equals("custom")){
            this.setVisibility(View.VISIBLE);
            return false;
        }else{
            this.setVisibility(View.GONE);
            return true;
        }
    }
    public void updateValue(){
        String value = "";
        switch(type){
            case COLOR_HOME:
                value = translator.getTeamColorLocal(getContext(), MainActivity.match.home.color);
                break;
            case COLOR_AWAY:
                value = translator.getTeamColorLocal(getContext(), MainActivity.match.away.color);
                break;
            case MATCH_TYPE:
                value = MainActivity.match.match_type;
                loadCustomMatchTypesSpinner();
                break;
            case PERIOD_TIME:
                if(hideForMatchType()) return;
                value += MainActivity.match.period_time;
                break;
            case PERIOD_COUNT:
                if(hideForMatchType()) return;
                value += MainActivity.match.period_count;
                break;
            case SINBIN:
                if(hideForMatchType()) return;
                value += MainActivity.match.sinbin;
                break;
            case POINTS_TRY:
                if(hideForMatchType()) return;
                value += MainActivity.match.points_try;
                break;
            case POINTS_CON:
                if(hideForMatchType()) return;
                value += MainActivity.match.points_con;
                break;
            case POINTS_GOAL:
                if(hideForMatchType()) return;
                value += MainActivity.match.points_goal;
                break;
            case SCREEN_ON:
                value = getContext().getString(MainActivity.screen_on ? R.string.on : R.string.off);
                break;
            case TIMER_TYPE:
                value = getContext().getString(MainActivity.timer_type == 1 ? R.string.timer_type_down : R.string.timer_type_up);
                break;
            case RECORD_PLAYER:
                value = getContext().getString(MainActivity.record_player ? R.string.on : R.string.off);
                break;
            case RECORD_PENS:
                value = getContext().getString(MainActivity.record_pens ? R.string.on : R.string.off);
                break;
            case HELP:
                return;
        }
        menuItemValue.setText(value);
    }
    public void click(){
        switch(type){
            case COLOR_HOME:
            case COLOR_AWAY:
                menuItemColor.performClick();
                menuItemColor.setVisibility(View.VISIBLE);
                break;
            case MATCH_TYPE:
                menuItemMatchType.performClick();
                menuItemMatchType.setVisibility(View.VISIBLE);
                break;
            case PERIOD_TIME:
            case PERIOD_COUNT:
            case SINBIN:
            case POINTS_TRY:
            case POINTS_CON:
            case POINTS_GOAL:
                menuItemNumbers.performClick();
                menuItemNumbers.setVisibility(View.VISIBLE);
                break;
            case SCREEN_ON:
                MainActivity.screen_on = !MainActivity.screen_on;
                updateValue();
                break;
            case TIMER_TYPE:
                MainActivity.timer_type = MainActivity.timer_type == 1 ? 0 : 1;
                updateValue();
                break;
            case RECORD_PLAYER:
                MainActivity.record_player = !MainActivity.record_player;
                updateValue();
                break;
            case RECORD_PENS:
                MainActivity.record_pens = !MainActivity.record_pens;
                updateValue();
                break;
            case HELP:
                Intent intent_showHelp = new Intent("com.windkracht8.rugbyrefereewatch");
                intent_showHelp.putExtra("intent_type", "showHelp");
                intent_showHelp.putExtra("help_version", -1);
                getContext().sendBroadcast(intent_showHelp);
                break;
        }
    }
    final AdapterView.OnItemSelectedListener itemSelectedListener = new AdapterView.OnItemSelectedListener(){
        @Override
        public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id){
            if(position == 0) return;
            newValue(position, parentView.getSelectedItem().toString());
            parentView.setVisibility(View.GONE);
        }
        @Override
        public void onNothingSelected(AdapterView<?> parent){}
    };
    public void newValue(int position, String value){
        switch(type){
            case COLOR_HOME:
                MainActivity.match.home.color = translator.getTeamColorSystem(getContext(), position);
                break;
            case COLOR_AWAY:
                MainActivity.match.away.color = translator.getTeamColorSystem(getContext(), position);
                break;
            case MATCH_TYPE:
                position--;
                MainActivity.match.match_type = translator.getMatchTypeSystem(getContext(), position, value);
                switch(position){
                    case 0://15s
                        MainActivity.match.period_time = 40;
                        MainActivity.match.period_count = 2;
                        MainActivity.match.sinbin = 10;
                        MainActivity.match.points_try = 5;
                        MainActivity.match.points_con = 2;
                        MainActivity.match.points_goal = 3;
                        break;
                    case 1://10s
                        MainActivity.match.period_time = 10;
                        MainActivity.match.period_count = 2;
                        MainActivity.match.sinbin = 2;
                        MainActivity.match.points_try = 5;
                        MainActivity.match.points_con = 2;
                        MainActivity.match.points_goal = 3;
                        break;
                    case 2://7s
                        MainActivity.match.period_time = 7;
                        MainActivity.match.period_count = 2;
                        MainActivity.match.sinbin = 2;
                        MainActivity.match.points_try = 5;
                        MainActivity.match.points_con = 2;
                        MainActivity.match.points_goal = 3;
                        break;
                    case 3://beach 7s
                        MainActivity.match.period_time = 7;
                        MainActivity.match.period_count = 2;
                        MainActivity.match.sinbin = 2;
                        MainActivity.match.points_try = 1;
                        MainActivity.match.points_con = 0;
                        MainActivity.match.points_goal = 0;
                        break;
                    case 4://beach 5s
                        MainActivity.match.period_time = 5;
                        MainActivity.match.period_count = 2;
                        MainActivity.match.sinbin = 2;
                        MainActivity.match.points_try = 1;
                        MainActivity.match.points_con = 0;
                        MainActivity.match.points_goal = 0;
                        break;
                    case 5://custom
                        break;
                    default://stored custom match type
                        loadCustomMatchType(value);
                }
                MainActivity.timer_period_time = MainActivity.match.period_time;
                Conf.updateValues();
                break;
            case PERIOD_TIME:
                MainActivity.match.period_time = position;
                MainActivity.timer_period_time = MainActivity.match.period_time;
                break;
            case PERIOD_COUNT:
                MainActivity.match.period_count = position;
                break;
            case SINBIN:
                MainActivity.match.sinbin = position;
                break;
            case POINTS_TRY:
                MainActivity.match.points_try = position-1;
                break;
            case POINTS_CON:
                MainActivity.match.points_con = position-1;
                break;
            case POINTS_GOAL:
                MainActivity.match.points_goal = position-1;
                break;
        }
        updateValue();
    }
    private void loadCustomMatchTypesSpinner(){
        if(Conf.customMatchTypes.length() == 0) return;
        try{
            ArrayList<String> alMatchTypes = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.matchTypes)));
            for(int i = 0; i < Conf.customMatchTypes.length(); i++){
                alMatchTypes.add(Conf.customMatchTypes.getJSONObject(i).getString("name"));
            }
            ArrayAdapter<String> aaMatchTypes = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, alMatchTypes);
            aaMatchTypes.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            menuItemMatchType.setAdapter(aaMatchTypes);
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "MenuItem.loadCustomMatchTypesSpinner Exception: " + e.getMessage());
            Toast.makeText(getContext(), R.string.fail_read_match_types, Toast.LENGTH_SHORT).show();
        }
    }
    private void loadCustomMatchType(String name){
        try{
            for(int i=0; i < Conf.customMatchTypes.length(); i++){
                JSONObject matchType = Conf.customMatchTypes.getJSONObject(i);
                if(matchType.getString("name").equals(name)){
                    MainActivity.match.period_time = matchType.getInt("period_time");
                    MainActivity.match.period_count = matchType.getInt("period_count");
                    MainActivity.match.sinbin = matchType.getInt("sinbin");
                    MainActivity.match.points_try = matchType.getInt("points_try");
                    MainActivity.match.points_con = matchType.getInt("points_con");
                    MainActivity.match.points_goal = matchType.getInt("points_goal");
                    return;
                }
            }
        }catch(Exception e){
            Log.e(MainActivity.RRW_LOG_TAG, "MenuItem.loadCustomMatchType Exception: " + e.getMessage());
            Toast.makeText(getContext(), R.string.fail_load_match_type, Toast.LENGTH_SHORT).show();
        }
    }
}
