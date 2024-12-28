package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ScrollView;

public class Help extends ScrollView{
    public Help(Context context, AttributeSet attrs){
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        inflater.inflate(R.layout.help, this, true);
    }
    void show(boolean withWelcome){
        if(withWelcome) findViewById(R.id.welcome).setVisibility(VISIBLE);
        setVisibility(View.VISIBLE);
        requestFocus();
        if(Main.isScreenRound) findViewById(R.id.llHelp).setPadding(Main._10dp, Main.vh25, Main._10dp, Main.vh25);
    }
}