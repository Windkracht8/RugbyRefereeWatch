package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class ConfSpinner extends ScrollView{
    private LinearLayout llConfSpinner;
    private TextView conf_spinner_label;
    private final ArrayList<TextView> confSpinnerItems = new ArrayList<>();
    private boolean isInitialized;
    private int itemHeight;
    private boolean isItemHeightInitialized = false;
    private float scalePerPixel;

    public ConfSpinner(Context context, AttributeSet attrs){
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(inflater == null){
            Toast.makeText(context, R.string.fail_show_conf, Toast.LENGTH_SHORT).show();
            return;
        }
        inflater.inflate(R.layout.conf_spinner, this, true);

        conf_spinner_label = findViewById(R.id.conf_spinner_label);
        llConfSpinner = findViewById(R.id.llConfSpinner);
    }
    void addOnTouch(Main main){
        main.addOnTouch(conf_spinner_label);
    }

    void setConfItemType(Main main, Conf conf, ConfItem confItem, ConfItem.ConfItemType confItemType){
        if(isInitialized){
            for(int i = llConfSpinner.getChildCount(); i>1; i--){
                llConfSpinner.removeViewAt(i-1);
            }
            confSpinnerItems.clear();
        }else{
            findViewById(R.id.conf_spinner_label).getLayoutParams().height = Main.vh25;
            ((LayoutParams) llConfSpinner.getLayoutParams()).bottomMargin += Main.vh25;
        }
        isInitialized = true;

        conf_spinner_label.setText(ConfItem.getConfItemName(confItemType));

        int intStart = 0;
        int intEnd = 0;
        switch(confItemType){
            case COLOR_HOME:
            case COLOR_AWAY:
                String[] teamColors = getContext().getResources().getStringArray(R.array.teamColors);
                String[] teamColors_system = getContext().getResources().getStringArray(R.array.teamColors_system);
                for(int i = 0; i < teamColors_system.length; i++){
                    TextView confSpinnerItem = newConfSpinnerItem(main, teamColors[i]);
                    String teamColor_system = teamColors_system[i];
                    confSpinnerItem.setOnClickListener(v -> conf.onStringValueClick(confItem, confItemType, teamColor_system));
                    llConfSpinner.addView(confSpinnerItem);
                }
                break;
            case MATCH_TYPE:
                String[] matchTypes = getContext().getResources().getStringArray(R.array.matchTypes);
                String[] matchTypes_system = getContext().getResources().getStringArray(R.array.matchTypes_system);
                for(int i = 0; i < matchTypes_system.length; i++){
                    TextView confSpinnerItem = newConfSpinnerItem(main, matchTypes[i]);
                    String matchType_system = matchTypes_system[i];
                    confSpinnerItem.setOnClickListener(v -> conf.onStringValueClick(confItem, confItemType, matchType_system));
                    llConfSpinner.addView(confSpinnerItem);
                }
                addCustomMatchTypes(main, conf, confItem, confItemType);
                break;
            case PERIOD_TIME:
                intStart = 1;
                intEnd = 50;
                break;
            case PERIOD_COUNT:
                intStart = 1;
                intEnd = 5;
                break;
            case SINBIN:
                intEnd = 20;
                break;
            case POINTS_TRY:
            case POINTS_CON:
            case POINTS_GOAL:
                intEnd = 9;
                break;
        }
        if(intEnd > 0){
            for(int i = intStart; i <= intEnd; i++){
                TextView confSpinnerItem = newConfSpinnerItem(main, String.valueOf(i));
                int value = i;
                confSpinnerItem.setOnClickListener(v -> conf.onIntValueClick(confItem, confItemType, value));
                llConfSpinner.addView(confSpinnerItem);
            }
        }
        fullScroll(View.FOCUS_UP);
        if(Main.isScreenRound){
            getViewTreeObserver().addOnGlobalLayoutListener(() -> scaleConfSpinnerItems(0));
        }
    }
    private void addCustomMatchTypes(Main main, Conf conf, ConfItem confItem, ConfItem.ConfItemType confItemType){
        if(Conf.customMatchTypes.length() == 0) return;
        try{
            for(int i=0; i<Conf.customMatchTypes.length(); i++){
                String name = Conf.customMatchTypes.getJSONObject(i).getString("name");
                TextView temp = newConfSpinnerItem(main, name);
                temp.setOnClickListener(v -> conf.onStringValueClick(confItem, confItemType, name));
                llConfSpinner.addView(temp);
            }
        }catch(Exception e){
            Log.e(Main.RRW_LOG_TAG, "ConfSpinner.addCustomMatchTypes Exception: " + e.getMessage());
            Toast.makeText(getContext(), R.string.fail_read_match_types, Toast.LENGTH_SHORT).show();
        }
    }
    private TextView newConfSpinnerItem(Main main, String text){
        TextView confSpinnerItem = new TextView(getContext());
        confSpinnerItem.setMinHeight(getResources().getDimensionPixelSize(R.dimen.minTouchSize));
        confSpinnerItem.setBackgroundResource(R.drawable.conf_item_bg);
        confSpinnerItem.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        confSpinnerItem.setGravity(Gravity.CENTER);
        confSpinnerItem.setText(text);
        confSpinnerItems.add(confSpinnerItem);
        main.addOnTouch(confSpinnerItem);
        return confSpinnerItem;
    }
    private void scaleConfSpinnerItems(int scrollY){
        if(!isItemHeightInitialized){
            itemHeight = confSpinnerItems.get(0).getHeight();
            scalePerPixel = 0.5f / (Main.vh25 + itemHeight);
            setOnScrollChangeListener((v, sX, sY, osX, osY) -> scaleConfSpinnerItems(sY));
            isItemHeightInitialized = true;
        }

        float top;
        float bottom;
        float scale;
        for(TextView confSpinnerItem : confSpinnerItems){
            top = confSpinnerItem.getY() - scrollY;
            bottom = top + itemHeight;
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
            confSpinnerItem.setScaleX(scale);
            confSpinnerItem.setScaleY(scale);
        }
    }
}
