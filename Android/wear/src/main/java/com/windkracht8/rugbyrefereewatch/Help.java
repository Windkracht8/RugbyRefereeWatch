package com.windkracht8.rugbyrefereewatch;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Help extends Activity{
    private LinearLayout list;
    @Override public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scroll_screen);
        list = findViewById(R.id.list);
        if(Main.isScreenRound) list.setPadding(Main.vh10, Main.vh15, Main.vh10, Main.vh25);
        ((TextView)findViewById(R.id.label)).setText(R.string.help);

        if(getIntent().getBooleanExtra("showWelcome", false)){
            findViewById(R.id.label).setVisibility(TextView.GONE);
            addTopic(getString(R.string.help_welcome_title), getString(R.string.help_welcome_text));
        }

        String[] help_topics_title = getResources().getStringArray(R.array.help_topics_title);
        String[] help_topics_text = getResources().getStringArray(R.array.help_topics_text);
        for(int i=0; i<help_topics_title.length; i++)
            addTopic(help_topics_title[i], help_topics_text[i]);
    }
    @Override public void onResume(){
        super.onResume();
        findViewById(R.id.scrollView).requestFocus();
    }
    private void addTopic(String title, String text){
        TextView tv_title = new TextView(this, null, 0, R.style.textView_help_title);
        tv_title.setText(title);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, Main._10dp, 0, 0);
        tv_title.setLayoutParams(params);
        list.addView(tv_title);
        TextView tv_text = new TextView(this, null, 0, R.style.textView_help_text);
        tv_text.setText(text);
        list.addView(tv_text);
    }
}