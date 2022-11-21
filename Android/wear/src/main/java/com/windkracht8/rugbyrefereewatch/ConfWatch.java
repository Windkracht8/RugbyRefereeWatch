package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;

public class ConfWatch extends LinearLayout {
    private SwitchCompat screen_on_cw;
    private Button timer_type_cw;
    private SwitchCompat record_player_cw;
    private SwitchCompat record_pens_cw;

    private static boolean fixed_height = false;
    public ConfWatch(Context context, AttributeSet attrs){
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(inflater == null){ Toast.makeText(context, R.string.fail_show_conf, Toast.LENGTH_SHORT).show(); return;}
        inflater.inflate(R.layout.conf_watch, this, true);

        TextView config_cw_text = findViewById(R.id.conf_watch_label);
        config_cw_text.setTextSize(TypedValue.COMPLEX_UNIT_PX, MainActivity.vh7);

        TextView timer_type_cw_text = findViewById(R.id.timer_type_cw_text);
        timer_type_cw_text.setOnClickListener(v -> toggleTimerType());
        timer_type_cw = findViewById(R.id.timer_type_cw);
        timer_type_cw.setOnClickListener(v -> toggleTimerType());
        timer_type_cw.setPadding(0,0,0,0);

        TextView record_player_cw_text = findViewById(R.id.record_player_cw_text);
        record_player_cw_text.setOnClickListener(v -> record_player_cw.toggle());
        record_player_cw = findViewById(R.id.record_player_cw);
        record_player_cw.getLayoutParams().height = 0;

        TextView record_pens_cw_text = findViewById(R.id.record_pens_cw_text);
        record_pens_cw_text.setOnClickListener(v -> record_pens_cw.toggle());
        record_pens_cw = findViewById(R.id.record_pens_cw);
        record_pens_cw.getLayoutParams().height = 0;

        TextView screen_on_cw_text = findViewById(R.id.screen_on_cw_text);
        screen_on_cw_text.setOnClickListener(v -> screen_on_cw.toggle());
        screen_on_cw = findViewById(R.id.screen_on_cw);
        screen_on_cw.getLayoutParams().height = 0;

        findViewById(R.id.record_player_cw).getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if(fixed_height || record_player_cw.getMeasuredHeight() == 0){return;}
            fixed_height = true;
            int height_rest = (MainActivity.heightPixels-(record_player_cw.getMeasuredHeight()*3))/8;

            timer_type_cw_text.setTextSize(TypedValue.COMPLEX_UNIT_PX, height_rest);
            timer_type_cw.setTextSize(TypedValue.COMPLEX_UNIT_PX, height_rest);
            record_player_cw_text.setTextSize(TypedValue.COMPLEX_UNIT_PX, height_rest);
            record_pens_cw_text.setTextSize(TypedValue.COMPLEX_UNIT_PX, height_rest);
            screen_on_cw_text.setTextSize(TypedValue.COMPLEX_UNIT_PX, height_rest);
        });
    }
    private void toggleTimerType(){
        if(MainActivity.timer_type == 1){
            MainActivity.timer_type = 0;
            timer_type_cw.setText(R.string.timer_type_up);
        }else{
            MainActivity.timer_type = 1;
            timer_type_cw.setText(R.string.timer_type_down);
        }
        MainActivity.match.timer_type = MainActivity.timer_type;
    }

    public void show(){
        timer_type_cw.setText(MainActivity.timer_type == 1 ? R.string.timer_type_down : R.string.timer_type_up);
        record_player_cw.setChecked(MainActivity.record_player);
        record_pens_cw.setChecked(MainActivity.record_pens);
        screen_on_cw.setChecked(MainActivity.screen_on);
        this.setVisibility(View.VISIBLE);
    }
    public void onBackPressed(){
        MainActivity.record_player = record_player_cw.isChecked();
        MainActivity.record_pens = record_pens_cw.isChecked();
        MainActivity.screen_on = screen_on_cw.isChecked();
        this.setVisibility(View.GONE);
    }
}
