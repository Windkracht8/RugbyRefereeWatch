package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class ConfSpinner extends ScrollView{
    private final LinearLayout llConfSpinner;
    private final TextView confSpinnerLabel;
    private final ArrayList<TextView> confSpinnerItems = new ArrayList<>();
    private boolean isInitialized;
    private static int itemHeight;
    private static float scalePerPixel = 0;
    private static float bottom_quarter;
    private static float below_screen;

    public ConfSpinner(Context context, AttributeSet attrs){
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        inflater.inflate(R.layout.conf_spinner, this, true);

        confSpinnerLabel = findViewById(R.id.confSpinnerLabel);
        llConfSpinner = findViewById(R.id.llConfSpinner);
    }

    void show(Main main, Conf conf, ConfItem confItem, ConfItem.ConfItemType confItemType){
        if(isInitialized){
            for(int i = llConfSpinner.getChildCount(); i>1; i--){
                llConfSpinner.removeViewAt(i-1);
            }
            confSpinnerItems.clear();
        }
        isInitialized = true;

        confSpinnerLabel.setText(ConfItem.getConfItemName(confItemType));

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
        if(Main.isScreenRound){
            getViewTreeObserver().addOnGlobalLayoutListener(()->{
                if(scalePerPixel > 0) return;
                itemHeight = confSpinnerItems.get(0).getHeight();
                bottom_quarter = Main.vh75 - itemHeight;
                below_screen = Main.heightPixels - itemHeight;
                scalePerPixel = 0.2f / Main.vh25;
                scaleItems(0);
                setOnScrollChangeListener((v, sx, sy, osx, osy)->scaleItems(sy));
            });
        }
        fullScroll(View.FOCUS_UP);
        setVisibility(View.VISIBLE);
        requestFocus();
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
            Log.e(Main.LOG_TAG, "ConfSpinner.addCustomMatchTypes Exception: " + e.getMessage());
            Toast.makeText(getContext(), R.string.fail_read_match_types, Toast.LENGTH_SHORT).show();
        }
    }
    private TextView newConfSpinnerItem(Main main, String text){
        TextView confSpinnerItem = new TextView(main, null, 0, R.style.textView_item);
        confSpinnerItem.setText(text);
        confSpinnerItems.add(confSpinnerItem);
        main.addOnTouch(confSpinnerItem);
        return confSpinnerItem;
    }
    private void scaleItems(int scrollY){
        float top;
        float scale;
        for(TextView item : confSpinnerItems){
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
}
