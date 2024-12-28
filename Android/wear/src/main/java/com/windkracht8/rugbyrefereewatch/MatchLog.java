package com.windkracht8.rugbyrefereewatch;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MatchLog extends ScrollView{
    private final LinearLayout llMatchLogItems;

    public MatchLog(Context context, AttributeSet attrs){
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        inflater.inflate(R.layout.match_log, this, true);
        llMatchLogItems = findViewById(R.id.llMatchLogItems);
    }

    void show(Main main, Report report){
        for(int i = llMatchLogItems.getChildCount(); i > 0; i--){
            llMatchLogItems.removeViewAt(i - 1);
        }
        JSONArray matches = FileStore.readMatches(main);
        try{
            for(int i = matches.length() - 1; i >= 0; i--){
                MatchData match = new MatchData(main, matches.getJSONObject(i));
                addNewItem(main, match, report);
            }
        }catch(JSONException e){
            Log.e(Main.LOG_TAG, "MatchLog.show Exception: " + e.getMessage());
            main.toast(R.string.fail_show_log);
        }

        setVisibility(View.VISIBLE);
        fullScroll(View.FOCUS_UP);
        requestFocus();
    }
    void onCreateMain(Main main){
        if(Main.isScreenRound){
            main.si_addLayout(this, llMatchLogItems);
            llMatchLogItems.setPadding(Main._10dp, 0, Main._10dp, Main.vh25);
            TextView label = findViewById(R.id.matchLogLabel);
            label.getLayoutParams().height = Main.vh30;
            label.setPadding(Main.vh10, Main.vh10, Main.vh10, 0);
        }
    }

    private void addNewItem(Main main, MatchData match, Report report){
        TextView item = new TextView(main, null, 0, R.style.textView_item);
        Date match_date_d = new Date(match.match_id);
        String text = new SimpleDateFormat("E dd MMM HH:mm", Locale.getDefault()).format(match_date_d);

        text += "\n";
        text += match.home.team.equals(match.home.id) ? match.home.color : match.home.team;
        text += " : ";
        text += match.away.team.equals(match.away.id) ? match.away.color : match.away.team;

        text += "\n";
        text += match.home.tot;
        text += " : ";
        text += match.away.tot;

        item.setText(text);
        item.setOnClickListener(v -> report.show(main, match));
        llMatchLogItems.addView(item);
        main.addOnTouch(item);
    }
}