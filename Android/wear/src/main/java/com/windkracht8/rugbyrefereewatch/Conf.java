package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class Conf extends ConstraintLayout{
    private final ScrollView svConf;
    ConfSpinner confSpinner;
    private static ArrayList<ConfItem> confItems;
    final static JSONArray customMatchTypes = new JSONArray();
    private boolean isInitialized;
    private static int itemHeight;
    private static float scalePerPixel = 0;
    private static float bottom_quarter;
    private static float below_screen;

    public Conf(Context context, AttributeSet attrs){
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        inflater.inflate(R.layout.conf, this, true);
        confSpinner = findViewById(R.id.confSpinner);
        svConf = findViewById(R.id.svConf);
    }

    void requestSVFocus(){svConf.requestFocus();}
    void show(Main main){
        if(isInitialized){
            setVisibility(View.VISIBLE);
            svConf.fullScroll(View.FOCUS_UP);
            svConf.requestFocus();
            return;
        }
        isInitialized = true;
        LinearLayout llConf = findViewById(R.id.llConf);
        confItems = new ArrayList<>();
        for(ConfItem.ConfItemType confItemType : ConfItem.ConfItemType.values()){
            ConfItem confItem = new ConfItem(getContext(), confItemType);
            confItem.setOnClickListener(v -> onConfItemClick(main, (ConfItem)v, confItemType));
            confItems.add(confItem);
            llConf.addView(confItem);
            main.addOnTouch(confItem);
        }

        if(Main.isScreenRound){
            getViewTreeObserver().addOnGlobalLayoutListener(()->{
                if(scalePerPixel > 0) return;
                itemHeight = confItems.get(0).getHeight();
                bottom_quarter = Main.vh75 - itemHeight;
                below_screen = Main.heightPixels - itemHeight;
                scalePerPixel = 0.2f / Main.vh25;
                scaleItems(0);
                svConf.setOnScrollChangeListener((v, sx, sy, osx, osy)->scaleItems(sy));
            });
        }

        updateValues();
        setVisibility(View.VISIBLE);
        svConf.fullScroll(View.FOCUS_UP);
        svConf.requestFocus();
    }

    private static void updateValues(){//Thread: Always on UI thread
        for(ConfItem confItem : confItems) confItem.updateValue();
    }

    private void scaleItems(int scrollY){
        float top;
        float scale;
        for(ConfItem item : confItems){
            top = item.getY() - scrollY;
            scale = 1.0f;
            if(top < 0){
                //the item is above the screen
                scale = 0.8f;
            }else if(top < Main.vh25){
                //the item is in the top quarter
                scale = 0.8f + (scalePerPixel * top);
            }else if(top > below_screen){
                //the item is below the screen
                scale = 0.8f;
            }else if(top > bottom_quarter){
                //the item is in the bottom quarter
                scale = 1.0f - (scalePerPixel * (top - bottom_quarter));
            }
            item.setScaleX(scale);
            item.setScaleY(scale);
        }
    }

    private void onConfItemClick(Main main, ConfItem confItem, ConfItem.ConfItemType type){
        switch(type){
            case COLOR_HOME:
            case COLOR_AWAY:
            case MATCH_TYPE:
            case PERIOD_TIME:
            case PERIOD_COUNT:
            case SINBIN:
            case POINTS_TRY:
            case POINTS_CON:
            case POINTS_GOAL:
                confSpinner.show(main, this, confItem, type);
                break;
            case SCREEN_ON:
                Main.screen_on = !Main.screen_on;
                confItem.updateValue();
                break;
            case TIMER_TYPE:
                Main.timer_type_period = Main.timer_type_period == 1 ? 0 : 1;
                confItem.updateValue();
                break;
            case RECORD_PLAYER:
                Main.record_player = !Main.record_player;
                confItem.updateValue();
                break;
            case RECORD_PENS:
                Main.record_pens = !Main.record_pens;
                confItem.updateValue();
                break;
            case HELP:
                main.showHelp();
                break;
            case COMMS_LOG:
                main.showCommsLog();
                break;
        }
    }

    void onStringValueClick(ConfItem confItem, ConfItem.ConfItemType type, String value){
        switch(type){
            case COLOR_HOME:
                Main.match.home.color = value;
                confItem.updateValue();
                break;
            case COLOR_AWAY:
                Main.match.away.color = value;
                confItem.updateValue();
                break;
            case MATCH_TYPE:
                Main.match.match_type = value;
                onMatchTypeChanged(value);
                break;
        }
        confItem.updateValue();
        confSpinner.setVisibility(View.GONE);
        svConf.requestFocus();
    }
    void onIntValueClick(ConfItem confItem, ConfItem.ConfItemType type, int value){
        switch(type){
            case PERIOD_TIME:
                Main.match.period_time = value;
                Main.timer_period_time = value;
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
        }
        confItem.updateValue();
        confSpinner.setVisibility(View.GONE);
        svConf.requestFocus();
    }
    private void onMatchTypeChanged(String matchType){//Thread: Always on UI thread
        switch(matchType){
            case "15s":
                Main.match.period_time = 40;
                Main.match.period_count = 2;
                Main.match.sinbin = 10;
                Main.match.points_try = 5;
                Main.match.points_con = 2;
                Main.match.points_goal = 3;
                break;
            case "10s":
                Main.match.period_time = 10;
                Main.match.period_count = 2;
                Main.match.sinbin = 2;
                Main.match.points_try = 5;
                Main.match.points_con = 2;
                Main.match.points_goal = 3;
                break;
            case "7s":
                Main.match.period_time = 7;
                Main.match.period_count = 2;
                Main.match.sinbin = 2;
                Main.match.points_try = 5;
                Main.match.points_con = 2;
                Main.match.points_goal = 3;
                break;
            case "beach 7s":
                Main.match.period_time = 7;
                Main.match.period_count = 2;
                Main.match.sinbin = 2;
                Main.match.points_try = 1;
                Main.match.points_con = 0;
                Main.match.points_goal = 0;
                break;
            case "beach 5s":
                Main.match.period_time = 5;
                Main.match.period_count = 2;
                Main.match.sinbin = 2;
                Main.match.points_try = 1;
                Main.match.points_con = 0;
                Main.match.points_goal = 0;
                break;
            case "custom":
                break;
            default://stored custom match type
                loadCustomMatchType(matchType);
        }
        Main.timer_period_time = Main.match.period_time;
        updateValues();
        if(Main.isScreenRound){
            confItems.get(0).getViewTreeObserver().addOnPreDrawListener(()->{
                scaleItems(svConf.getScrollY());
                return true;
            });
        }
    }
    private void loadCustomMatchType(String name){//Thread: Always on UI thread
        try{
            for(int i=0; i < Conf.customMatchTypes.length(); i++){
                JSONObject matchType = Conf.customMatchTypes.getJSONObject(i);
                if(matchType.getString("name").equals(name)){
                    Main.match.period_time = matchType.getInt("period_time");
                    Main.match.period_count = matchType.getInt("period_count");
                    Main.match.sinbin = matchType.getInt("sinbin");
                    Main.match.points_try = matchType.getInt("points_try");
                    Main.match.points_con = matchType.getInt("points_con");
                    Main.match.points_goal = matchType.getInt("points_goal");
                    return;
                }
            }
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "Conf.loadCustomMatchType(" + name + ") Exception: " + e.getMessage());
            Toast.makeText(getContext(), R.string.fail_load_match_type, Toast.LENGTH_SHORT).show();
        }
    }
    static void syncCustomMatchTypes(Main main, String request_data){//Thread: Always on background thread
        try{
            JSONObject request_data_jo = new JSONObject(request_data);
            if(!request_data_jo.has("custom_match_types")) return;
            JSONArray customMatchTypes_phone = request_data_jo.getJSONArray("custom_match_types");

            for(int l = customMatchTypes.length() - 1; l >= 0; l--){
                JSONObject matchType = customMatchTypes.getJSONObject(l);
                boolean found = false;
                for(int p = 0; p < customMatchTypes_phone.length(); p++){
                    JSONObject matchType_phone = customMatchTypes_phone.getJSONObject(p);
                    if(matchType_phone.getString("name").equals(matchType.getString("name"))){
                        found = true;
                        break;
                    }
                }
                if(!found) customMatchTypes.remove(l);
            }
            for(int p = 0; p < customMatchTypes_phone.length(); p++){
                JSONObject matchType_phone = customMatchTypes_phone.getJSONObject(p);
                boolean found = false;
                for(int l = 0; l < customMatchTypes.length(); l++){
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
            FileStore.storeCustomMatchTypes(main);
        }catch(Exception e){
            Log.e(Main.LOG_TAG, "Conf.syncCustomMatchTypes Exception: " + e.getMessage());
            main.toast(R.string.fail_sync_match_types);
        }
    }
}
