package com.windkracht8.rugbyrefereewatch;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

public class Help extends Activity{
    @Override public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help);
        if(getIntent().getBooleanExtra("showWelcome", false)) findViewById(R.id.welcome).setVisibility(View.VISIBLE);
        if(Main.isScreenRound) findViewById(R.id.llHelp).setPadding(Main._10dp, Main.vh15, Main._10dp, Main.vh25);
        findViewById(R.id.svHelp).requestFocus();
    }
}