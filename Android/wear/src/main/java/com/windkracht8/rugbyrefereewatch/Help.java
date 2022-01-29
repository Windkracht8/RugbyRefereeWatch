package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ScrollView;
import android.widget.Toast;

public class Help extends ScrollView{
    public Help(Context context, AttributeSet attrs){
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(inflater == null){Toast.makeText(context, "Failed to show help", Toast.LENGTH_SHORT).show(); return;}
        inflater.inflate(R.layout.help, this, true);
        findViewById(R.id.llHelp).setOnClickListener(v -> this.setVisibility(GONE));
    }
    public void showWelcome(){
        findViewById(R.id.welcome).setVisibility(VISIBLE);
    }
}