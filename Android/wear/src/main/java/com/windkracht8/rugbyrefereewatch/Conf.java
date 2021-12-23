package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;

public class Conf extends ScrollView {
    private final Spinner color_home;
    private final Spinner color_away;
    private final Spinner match_type;
    private final Spinner period_time;
    private final Spinner period_count;
    private final Spinner sinbin;
    private final Spinner points_try;
    private final Spinner points_con;
    private final Spinner points_goal;
    private final Switch screen_on;
    private final Spinner countdown;

    public Conf(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        inflater.inflate(R.layout.conf, this, true);

        color_home = findViewById(R.id.color_home);
        color_away = findViewById(R.id.color_away);
        match_type = findViewById(R.id.match_type);
        period_time = findViewById(R.id.period_time);
        period_count = findViewById(R.id.period_count);
        sinbin = findViewById(R.id.sinbin);
        points_try = findViewById(R.id.points_try);
        points_con = findViewById(R.id.points_con);
        points_goal = findViewById(R.id.points_goal);
        screen_on = findViewById(R.id.screen_on);
        countdown = findViewById(R.id.countdown);

        String[] aTemp = new String[] {"15s", "10s", "7s", "beach 7s", "beach 5s", "custom"};
        ArrayAdapter<String> aaTemp = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, aTemp);
        aaTemp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        match_type.setAdapter(aaTemp);
        match_type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
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
                }
                if(position != 5){
                    findViewById(R.id.custom_match).setVisibility(View.GONE);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {}
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

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context, R.array.teamcolors, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        color_home.setAdapter(adapter);
        color_away.setAdapter(adapter);

        String[] aCountType = new String[] {"down", "up"};
        ArrayAdapter<String> aaCountType = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, aCountType);
        aaCountType.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        countdown.setAdapter(aaCountType);
    }

    public void load(matchdata match){
        selectitem(color_home, match.home.color);
        selectitem(color_away, match.away.color);
        selectitem(match_type, match.match_type);

        period_time.setSelection(match.period_time-1);
        period_count.setSelection(match.period_count-1);
        sinbin.setSelection(match.sinbin-1);
        points_try.setSelection(match.points_try);
        points_con.setSelection(match.points_con);
        points_goal.setSelection(match.points_goal);

        screen_on.setChecked(MainActivity.screen_on);
        if(MainActivity.countdown){
            selectitem(countdown, "down");
        }else{
            selectitem(countdown, "up");
        }
    }
    private void selectitem(Spinner spin, String str){
        for (int i=0;i<spin.getCount();i++){
            if (spin.getItemAtPosition(i).equals(str)){
                spin.setSelection(i);
                return;
            }
        }
    }
    public void save(matchdata match){
        match.home.color = color_home.getSelectedItem().toString();
        match.away.color = color_away.getSelectedItem().toString();
        match.match_type = match_type.getSelectedItem().toString();

        match.period_time = period_time.getSelectedItemPosition()+1;
        match.period_count = period_count.getSelectedItemPosition()+1;
        match.sinbin = sinbin.getSelectedItemPosition()+1;
        match.points_try = points_try.getSelectedItemPosition();
        match.points_con = points_con.getSelectedItemPosition();
        match.points_goal = points_goal.getSelectedItemPosition();

        MainActivity.screen_on = screen_on.isChecked();
        MainActivity.countdown = countdown.getSelectedItem().toString().equals("down");
    }
}
