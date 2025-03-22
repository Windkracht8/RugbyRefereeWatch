package com.windkracht8.rugbyrefereewatch;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MatchLog extends ScrollScreen{
    @Override public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        label.setText(R.string.match_log_title);
        try{
            JSONArray matches = FileStore.readMatches(this);
            for(int i = matches.length() - 1; i >= 0; i--){
                MatchData match = new MatchData(matches.getJSONObject(i));
                addItem(match);
            }
        }catch(JSONException e){
            Log.e(Main.LOG_TAG, "MatchLog.show Exception: " + e.getMessage());
            Toast.makeText(this, R.string.fail_show_log, Toast.LENGTH_SHORT).show();
        }
    }

    private void addItem(MatchData match){
        TextView item = new TextView(this, null, 0, R.style.textView_item);
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
        item.setOnClickListener(v->{
            Report.match = match;
            startActivity(new Intent(this, Report.class));
        });
        list.addView(item);
    }
}