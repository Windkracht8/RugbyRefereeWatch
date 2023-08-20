package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class Conf extends ScrollView{
    private static ArrayList<MenuItem> menuItems;
    public static JSONArray customMatchTypes;
    private boolean isInitialized;
    private int menuItemHeight = 112;
    private boolean menuItemHeightInit;
    private float scalePerPixel;

    public Conf(Context context, AttributeSet attrs){
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(inflater == null){
            Toast.makeText(context, R.string.fail_show_conf, Toast.LENGTH_SHORT).show();
            return;
        }
        inflater.inflate(R.layout.conf, this, true);

        customMatchTypes = new JSONArray();
    }

    public void show(Main main){
        if(isInitialized){
            setVisibility(View.VISIBLE);
            fullScroll(View.FOCUS_UP);
            return;
        }
        isInitialized = true;
        LinearLayout llConf = findViewById(R.id.llConf);
        menuItems = new ArrayList<>();
        for(MenuItem.MenuItemType menuItemType : MenuItem.MenuItemType.values()){
            MenuItem menuItem = new MenuItem(getContext(), main.handler_message, null, menuItemType);
            menuItems.add(menuItem);
            llConf.addView(menuItem);
            menuItem.addOnTouch(main);
        }
        findViewById(R.id.conf_label).getLayoutParams().height = Main.vh25;
        ((LayoutParams) llConf.getLayoutParams()).bottomMargin = getResources().getDimensionPixelSize(R.dimen.llConf_padding) + Main.vh25;

        getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if(!Main.isScreenRound || menuItemHeightInit) return;
            menuItemHeightInit = true;
            menuItemHeight = menuItems.get(0).getHeight();
            scalePerPixel = 0.5f / (Main.vh25 + menuItemHeight);
            menuItems.get(MenuItem.MenuItemType.values().length - 1).setHeight(menuItemHeight / 4);
            scaleMenuItems(0);
            setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> scaleMenuItems(scrollY));
        });

        setVisibility(View.VISIBLE);
        fullScroll(View.FOCUS_UP);
    }

    public static void updateValues(){
        for(MenuItem menuItem : menuItems){
            menuItem.updateValue();
        }
    }

    private void scaleMenuItems(int scrollY){
        float top;
        float bottom;
        float scale;
        for(int i = 0; i < menuItems.size(); i++){
            MenuItem menuItem = menuItems.get(i);
            top = menuItem.getY() - scrollY;
            bottom = top + menuItemHeight;
            scale = 1.0f;
            if(bottom < 0){
                //the item is above the screen
                scale = 0.5f;
            }else if(top < Main.vh25){
                //the item is in the top quarter
                scale = 0.5f + (scalePerPixel * bottom);
            }
            if(top > Main.heightPixels){
                //the item is below the screen
                scale = 0.5f;
            }else if(bottom > Main.heightPixels - Main.vh25){
                //the item is in the bottom quarter
                scale = 0.5f + (scalePerPixel * (Main.heightPixels - top));
            }
            menuItem.setScaleX(scale);
            menuItem.setScaleY(scale);
        }
    }

    public static void syncCustomMatchTypes(Handler handler_message, String request_data){
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
        }catch(Exception e){
            Log.e(Main.RRW_LOG_TAG, "Conf.syncCustomMatchTypes Exception: " + e.getMessage());
            handler_message.sendMessage(handler_message.obtainMessage(Main.MESSAGE_TOAST, R.string.fail_sync_match_types));
        }
    }

}
