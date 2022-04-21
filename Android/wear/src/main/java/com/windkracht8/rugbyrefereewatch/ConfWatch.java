package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;

public class ConfWatch extends LinearLayout {
    private SwitchCompat record_player_cw;
    private SwitchCompat screen_on_cw;
    private Button timer_type_cw;

    public ConfWatch(Context context, AttributeSet attrs){
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(inflater == null){ Toast.makeText(context, "Failed to show conf screen", Toast.LENGTH_SHORT).show(); return;}
        inflater.inflate(R.layout.conf_watch, this, true);

        record_player_cw = findViewById(R.id.record_player_cw);
        screen_on_cw = findViewById(R.id.screen_on_cw);
        timer_type_cw = findViewById(R.id.timer_type_cw);

        timer_type_cw.setOnClickListener(v -> {
            if(MainActivity.timer_type == 1){
                MainActivity.timer_type = 0;
                timer_type_cw.setText(R.string.timer_type_up);
            }else{
                MainActivity.timer_type = 1;
                timer_type_cw.setText(R.string.timer_type_down);
            }
        });
    }

    public void show(){
        record_player_cw.setChecked(MainActivity.record_player);
        screen_on_cw.setChecked(MainActivity.screen_on);
        timer_type_cw.setText(MainActivity.timer_type == 1 ? R.string.timer_type_down : R.string.timer_type_up);
        this.setVisibility(View.VISIBLE);
    }
    public void onBackPressed(){
        MainActivity.record_player = record_player_cw.isChecked();
        MainActivity.screen_on = screen_on_cw.isChecked();
        this.setVisibility(View.GONE);
    }
}
