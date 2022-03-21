package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class Goal extends LinearLayout{
    public Goal(Context context, AttributeSet attrs){
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(inflater == null){Toast.makeText(context, "Failed to show goal screen", Toast.LENGTH_SHORT).show(); return;}
        inflater.inflate(R.layout.goal, this, true);

        ((TextView)findViewById(R.id.goal_top_space)).setHeight(MainActivity.vh15);
        ((TextView)findViewById(R.id.goal_pen)).setHeight(MainActivity.vh30);
        ((TextView)findViewById(R.id.goal_drop)).setHeight(MainActivity.vh30);
    }
}
