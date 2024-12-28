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

public class ConfSpinner extends ScrollView{
    private final LinearLayout llConfSpinner;
    private final TextView confSpinnerLabel;
    private boolean isInitialized;

    public ConfSpinner(Context context, AttributeSet attrs){
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        inflater.inflate(R.layout.conf_spinner, this, true);

        confSpinnerLabel = findViewById(R.id.confSpinnerLabel);
        llConfSpinner = findViewById(R.id.llConfSpinner);
    }
    void onCreateMain(Main main){
        if(Main.isScreenRound){
            main.si_addLayout(this, llConfSpinner);
            llConfSpinner.setPadding(Main._10dp, 0, Main._10dp, Main.vh25);
            TextView label = findViewById(R.id.confSpinnerLabel);
            label.getLayoutParams().height = Main.vh30;
            label.setPadding(Main.vh10, Main.vh10, Main.vh10, 0);
        }
    }

    void show(Main main, Conf conf, ConfItem confItem, ConfItem.ConfItemType confItemType){
        if(isInitialized){
            for(int i = llConfSpinner.getChildCount(); i>1; i--){
                llConfSpinner.removeViewAt(i-1);
            }
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

        fullScroll(View.FOCUS_UP);
        setVisibility(View.VISIBLE);
        main.si_scaleItemsAfterChange(llConfSpinner, this);
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
        main.addOnTouch(confSpinnerItem);
        return confSpinnerItem;
    }
}
